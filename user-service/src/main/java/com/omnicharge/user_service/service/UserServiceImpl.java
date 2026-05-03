package com.omnicharge.user_service.service;

import com.omnicharge.user_service.dto.*;
import com.omnicharge.user_service.entity.Role;
import com.omnicharge.user_service.entity.User;
import com.omnicharge.user_service.exception.*;
import com.omnicharge.user_service.repository.UserRepository;
import com.omnicharge.user_service.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Value("${app.admin.secret-key}")
	private String adminSecretKey;

	// ── Helper: build and save a new user 

	private User buildAndSaveUser(String username, String email, String password,
								  String fullName, String phone, Role role) {
		if (userRepository.existsByUsername(username)) {
			throw new UserAlreadyExistsException("Username already exists: " + username);
		}
		if (userRepository.existsByEmail(email)) {
			throw new UserAlreadyExistsException("Email already exists: " + email);
		}

		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(password));
		user.setFullName(fullName);
		user.setPhone(phone);
		user.setActive(true);
		user.setRole(role);

		return userRepository.save(user);
	}

	// ── Registration 

	@Override
	public AuthResponse register(RegisterRequest request) {
		logger.info("User registration attempt for username: {}", request.getUsername());

		User user = buildAndSaveUser(
				request.getUsername(), request.getEmail(), request.getPassword(),
				request.getFullName(), request.getPhone(), Role.ROLE_USER);

		String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getEmail());
		logger.info("User registered successfully: {}", user.getUsername());
		return new AuthResponse(token, user.getUsername(), user.getRole().name(), "User registered successfully");
	}

	@Override
	public AuthResponse registerAdmin(AdminRegisterRequest request) {
		logger.info("Admin registration attempt for username: {}", request.getUsername());

		if (request.getAdminSecretKey() == null || !request.getAdminSecretKey().equals(adminSecretKey)) {
			logger.warn("Admin registration rejected — invalid secret key for username: {}", request.getUsername());
			throw new InvalidCredentialsException("Invalid admin secret key");
		}

		User user = buildAndSaveUser(
				request.getUsername(), request.getEmail(), request.getPassword(),
				request.getFullName(), request.getPhone(), Role.ROLE_ADMIN);

		String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getEmail());
		logger.info("Admin registered successfully: {}", user.getUsername());
		return new AuthResponse(token, user.getUsername(), user.getRole().name(), "Admin registered successfully");
	}

	// ── Separate login endpoints ─────────────────────────────────────────────

	@Override
	public AuthResponse loginUser(LoginRequest request) {
		logger.info("User login attempt for username: {}", request.getUsername());

		User user = authenticateAndFetch(request.getUsername(), request.getPassword());

		// Issue ROLE_USER even if they are an admin, so they get user permissions
		String roleToIssue = Role.ROLE_USER.name();

		String token = jwtUtil.generateToken(user.getUsername(), roleToIssue, user.getEmail());
		logger.info("User login successful: {}", user.getUsername());
		return new AuthResponse(token, user.getUsername(), roleToIssue, "Login successful");
	}

	@Override
	public AuthResponse loginAdmin(LoginRequest request) {
		logger.info("Admin login attempt for username: {}", request.getUsername());

		User user = authenticateAndFetch(request.getUsername(), request.getPassword());

		// Reject if the account is a regular user — users must use the user login endpoint
		if (user.getRole() != Role.ROLE_ADMIN) {
			logger.warn("Non-admin account '{}' attempted login via admin endpoint", request.getUsername());
			throw new InvalidCredentialsException("Invalid credentials");
		}

		String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getEmail());
		logger.info("Admin login successful: {}", user.getUsername());
		return new AuthResponse(token, user.getUsername(), user.getRole().name(), "Login successful");
	}

	/**
	 * Shared helper: authenticate via Spring Security and fetch the User entity.
	 */
	private User authenticateAndFetch(String username, String password) {
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(username, password));
		} catch (AuthenticationException ex) {
			logger.warn("Authentication failed for username: {}", username);
			throw new InvalidCredentialsException("Invalid username or password");
		}

		return userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	// ── Profile & Admin ops ──────────────────────────────────────────────────

	@Override
	public UserDto getUserProfile(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
		return mapToDto(user);
	}

	@Override
	public List<UserDto> getAllUsers() {
		return userRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
	}

	@Override
	public UserDto promoteToAdmin(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

		if (user.getRole() == Role.ROLE_ADMIN) {
			throw new BadRequestException("User is already an admin");
		}

		user.setRole(Role.ROLE_ADMIN);
		userRepository.save(user);
		logger.info("User '{}' promoted to ADMIN", user.getUsername());
		return mapToDto(user);
	}

	// ── Password change (with null-safety guards) ────────────────────────────

	@Override
	public String changePassword(String username, ChangePasswordRequest request) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

		validatePasswordChange(request, user);

		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);

		logger.info("Password changed successfully for user: {}", username);
		return "Password changed successfully";
	}

	/**
	 * Extracted password validation to reduce cognitive complexity (SonarQube Ex 17).
	 */
	private void validatePasswordChange(ChangePasswordRequest request, User user) {
		if (request.getCurrentPassword() == null || request.getNewPassword() == null
				|| request.getConfirmPassword() == null) {
			throw new BadRequestException("All password fields are required");
		}

		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
			throw new BadRequestException("Current password is incorrect");
		}

		if (!request.getNewPassword().equals(request.getConfirmPassword())) {
			throw new BadRequestException("New password and confirm password do not match");
		}

		if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
			throw new BadRequestException("New password must be different from current password");
		}

		if (request.getNewPassword().length() < 6) {
			throw new BadRequestException("New password must be at least 6 characters");
		}
	}

	@Override
	public java.math.BigDecimal getWalletBalance(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
		return user.getWalletBalance();
	}

	@Override
	public void updateWalletBalance(String username, java.math.BigDecimal amount, boolean isTopUp) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
		
		if (isTopUp) {
			user.setWalletBalance(user.getWalletBalance().add(amount));
		} else {
			if (user.getWalletBalance().compareTo(amount) < 0) {
				throw new BadRequestException("Insufficient wallet balance");
			}
			user.setWalletBalance(user.getWalletBalance().subtract(amount));
		}
		userRepository.save(user);
		logger.info("Wallet balance updated for user '{}'. New balance: {}", username, user.getWalletBalance());
	}

	// ── DTO mapper ───────────────────────────────────────────────────────────

	private UserDto mapToDto(User user) {
		UserDto dto = new UserDto();
		dto.setId(user.getId());
		dto.setUsername(user.getUsername());
		dto.setEmail(user.getEmail());
		dto.setFullName(user.getFullName());
		dto.setPhone(user.getPhone());
		dto.setRole(user.getRole().name());
		dto.setActive(user.isActive());
		dto.setWalletBalance(user.getWalletBalance());
		return dto;
	}
}