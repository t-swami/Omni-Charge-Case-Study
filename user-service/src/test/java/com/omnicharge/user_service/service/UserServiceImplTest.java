package com.omnicharge.user_service.service;

import com.omnicharge.user_service.dto.*;
import com.omnicharge.user_service.entity.Role;
import com.omnicharge.user_service.entity.User;
import com.omnicharge.user_service.exception.*;
import com.omnicharge.user_service.repository.UserRepository;
import com.omnicharge.user_service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User buildUser(Long id, String username, Role role) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@test.com");
        u.setPassword("encoded_pass");
        u.setFullName("Full " + username);
        u.setPhone("9876543210");
        u.setRole(role);
        u.setActive(true);
        return u;
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "adminSecretKey", "omniCharge008");
    }

    // ── register() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register() - success: new user is saved and JWT returned")
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice@test.com");
        req.setPassword("pass123");
        req.setFullName("Alice");
        req.setPhone("9876543210");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken("alice", "ROLE_USER", "alice@test.com")).thenReturn("jwt.token.alice");

        AuthResponse response = userService.register(req);

        assertThat(response.getToken()).isEqualTo("jwt.token.alice");
        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register() - fail: duplicate username throws UserAlreadyExistsException")
    void register_duplicateUsername_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice@test.com");
        req.setPassword("pass123");
        req.setFullName("Alice");
        req.setPhone("9876543210");

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() - fail: duplicate email throws UserAlreadyExistsException")
    void register_duplicateEmail_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice@test.com");
        req.setPassword("pass123");
        req.setFullName("Alice");
        req.setPhone("9876543210");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Email already exists");
    }

    // ── registerAdmin() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("registerAdmin() - success: correct secret key, ROLE_ADMIN assigned")
    void registerAdmin_success() {
        AdminRegisterRequest req = new AdminRegisterRequest();
        req.setUsername("bob");
        req.setEmail("bob@test.com");
        req.setPassword("adminpass");
        req.setFullName("Bob Admin");
        req.setPhone("9123456780");
        req.setAdminSecretKey("omniCharge008");

        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@test.com")).thenReturn(false);
        when(passwordEncoder.encode("adminpass")).thenReturn("encoded_admin");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken("bob", "ROLE_ADMIN", "bob@test.com")).thenReturn("jwt.admin.bob");

        AuthResponse response = userService.registerAdmin(req);

        assertThat(response.getRole()).isEqualTo("ROLE_ADMIN");
        assertThat(response.getToken()).isEqualTo("jwt.admin.bob");
    }

    @Test
    @DisplayName("registerAdmin() - fail: wrong secret key throws InvalidCredentialsException")
    void registerAdmin_wrongSecretKey_throwsException() {
        AdminRegisterRequest req = new AdminRegisterRequest();
        req.setUsername("hacker");
        req.setEmail("hacker@test.com");
        req.setPassword("hackpass");
        req.setFullName("Hacker");
        req.setPhone("9000000000");
        req.setAdminSecretKey("wrong_key");

        assertThatThrownBy(() -> userService.registerAdmin(req))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid admin secret key");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerAdmin() - fail: null secret key throws InvalidCredentialsException")
    void registerAdmin_nullSecretKey_throwsException() {
        AdminRegisterRequest req = new AdminRegisterRequest();
        req.setUsername("bob");
        req.setEmail("bob@test.com");
        req.setPassword("adminpass");
        req.setFullName("Bob");
        req.setPhone("9123456780");
        req.setAdminSecretKey(null);

        assertThatThrownBy(() -> userService.registerAdmin(req))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid admin secret key");
    }

    // ── loginUser() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("loginUser() - success: ROLE_USER returns JWT")
    void loginUser_success() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("pass123");

        User user = buildUser(1L, "alice", Role.ROLE_USER);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("alice", "ROLE_USER", "alice@test.com")).thenReturn("jwt.login.alice");

        AuthResponse response = userService.loginUser(req);

        assertThat(response.getToken()).isEqualTo("jwt.login.alice");
        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("alice", "pass123"));
    }

    @Test
    @DisplayName("loginUser() - success: admin account login as user returns ROLE_USER JWT")
    void loginUser_adminAccount_success() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin_user");
        req.setPassword("adminpass");

        User adminUser = buildUser(2L, "admin_user", Role.ROLE_ADMIN);

        when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(adminUser));
        when(jwtUtil.generateToken("admin_user", "ROLE_USER", "admin_user@test.com")).thenReturn("jwt.login.admin_as_user");

        AuthResponse response = userService.loginUser(req);

        assertThat(response.getToken()).isEqualTo("jwt.login.admin_as_user");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("loginUser() - fail: bad credentials throw InvalidCredentialsException")
    void loginUser_badCredentials_throwsException() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("wrong");

        doThrow(new AuthenticationException("Bad credentials") {})
                .when(authenticationManager)
                .authenticate(any());

        assertThatThrownBy(() -> userService.loginUser(req))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid username or password");
    }

    // ── loginAdmin() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("loginAdmin() - success: ROLE_ADMIN returns JWT")
    void loginAdmin_success() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin_user");
        req.setPassword("adminpass");

        User adminUser = buildUser(2L, "admin_user", Role.ROLE_ADMIN);

        when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(adminUser));
        when(jwtUtil.generateToken("admin_user", "ROLE_ADMIN", "admin_user@test.com")).thenReturn("jwt.admin.token");

        AuthResponse response = userService.loginAdmin(req);

        assertThat(response.getToken()).isEqualTo("jwt.admin.token");
        assertThat(response.getRole()).isEqualTo("ROLE_ADMIN");
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("admin_user", "adminpass"));
    }

    @Test
    @DisplayName("loginAdmin() - fail: user account trying admin endpoint throws InvalidCredentialsException")
    void loginAdmin_userAccount_throwsException() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("pass123");

        User normalUser = buildUser(1L, "alice", Role.ROLE_USER);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> userService.loginAdmin(req))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    @DisplayName("loginAdmin() - fail: bad credentials throw InvalidCredentialsException")
    void loginAdmin_badCredentials_throwsException() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin_user");
        req.setPassword("wrong");

        doThrow(new AuthenticationException("Bad credentials") {})
                .when(authenticationManager)
                .authenticate(any());

        assertThatThrownBy(() -> userService.loginAdmin(req))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid username or password");
    }

    // ── getUserProfile() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserProfile() - success: returns DTO for existing user")
    void getUserProfile_success() {
        User user = buildUser(1L, "alice", Role.ROLE_USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDto dto = userService.getUserProfile("alice");

        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("getUserProfile() - fail: unknown user throws ResourceNotFoundException")
    void getUserProfile_notFound_throwsException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile("ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ── getAllUsers() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllUsers() - returns list of all user DTOs")
    void getAllUsers_returnsList() {
        User u1 = buildUser(1L, "alice", Role.ROLE_USER);
        User u2 = buildUser(2L, "bob", Role.ROLE_ADMIN);
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<UserDto> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUsername()).isEqualTo("alice");
        assertThat(result.get(1).getUsername()).isEqualTo("bob");
    }

    // ── promoteToAdmin() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("promoteToAdmin() - success: USER is promoted to ADMIN")
    void promoteToAdmin_success() {
        User user = buildUser(1L, "alice", Role.ROLE_USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto dto = userService.promoteToAdmin(1L);

        assertThat(dto.getRole()).isEqualTo("ROLE_ADMIN");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("promoteToAdmin() - fail: already ADMIN throws BadRequestException")
    void promoteToAdmin_alreadyAdmin_throwsException() {
        User user = buildUser(1L, "bob", Role.ROLE_ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.promoteToAdmin(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already an admin");
    }

    @Test
    @DisplayName("promoteToAdmin() - fail: user not found throws ResourceNotFoundException")
    void promoteToAdmin_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.promoteToAdmin(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ── changePassword() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("changePassword() - success: valid request updates password")
    void changePassword_success() {
        User user = buildUser(1L, "alice", Role.ROLE_USER);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("oldpass");
        req.setNewPassword("newpass123");
        req.setConfirmPassword("newpass123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldpass", "encoded_pass")).thenReturn(true);
        when(passwordEncoder.matches("newpass123", "encoded_pass")).thenReturn(false);
        when(passwordEncoder.encode("newpass123")).thenReturn("encoded_new");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String msg = userService.changePassword("alice", req);

        assertThat(msg).isEqualTo("Password changed successfully");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("changePassword() - fail: wrong current password")
    void changePassword_wrongCurrentPassword_throwsException() {
        User user = buildUser(1L, "alice", Role.ROLE_USER);
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("wrongpass");
        req.setNewPassword("newpass123");
        req.setConfirmPassword("newpass123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "encoded_pass")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("alice", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    @DisplayName("changePassword() - fail: new and confirm password mismatch")
    void changePassword_mismatch_throwsException() {
        User user = buildUser(1L, "alice", Role.ROLE_USER);
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("oldpass");
        req.setNewPassword("newpass123");
        req.setConfirmPassword("differentpass");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldpass", "encoded_pass")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword("alice", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    @DisplayName("changePassword() - fail: new password same as current")
    void changePassword_sameAsOld_throwsException() {
        User user = buildUser(1L, "alice", Role.ROLE_USER);
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("samepass");
        req.setNewPassword("samepass");
        req.setConfirmPassword("samepass");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("samepass", "encoded_pass")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword("alice", req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("changePassword() - fail: new password too short (< 6 chars)")
    void changePassword_tooShort_throwsException() {
        User user = buildUser(1L, "alice", Role.ROLE_USER);
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("oldpass");
        req.setNewPassword("abc");
        req.setConfirmPassword("abc");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldpass", "encoded_pass")).thenReturn(true);
        when(passwordEncoder.matches("abc", "encoded_pass")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("alice", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 6 characters");
    }

    @Test
    @DisplayName("changePassword() - fail: null password fields throw BadRequestException")
    void changePassword_nullFields_throwsException() {
        User user = buildUser(1L, "alice", Role.ROLE_USER);
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword(null);
        req.setNewPassword(null);
        req.setConfirmPassword(null);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePassword("alice", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("All password fields are required");
    }

    // ── Wallet Balance ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getWalletBalance() - success")
    void getWalletBalance_success() {
        User user = buildUser(1L, "alice", Role.ROLE_USER);
        user.setWalletBalance(new java.math.BigDecimal("200.50"));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        java.math.BigDecimal balance = userService.getWalletBalance("alice");
        assertThat(balance).isEqualTo(new java.math.BigDecimal("200.50"));
    }

    @Test
    @DisplayName("updateWalletBalance() - topup success")
    void updateWalletBalance_topup_success() {
        User user = buildUser(1L, "alice", Role.ROLE_USER);
        user.setWalletBalance(new java.math.BigDecimal("100.00"));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        userService.updateWalletBalance("alice", new java.math.BigDecimal("50.00"), true);

        assertThat(user.getWalletBalance()).isEqualTo(new java.math.BigDecimal("150.00"));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateWalletBalance() - deduction success")
    void updateWalletBalance_deduction_success() {
        User user = buildUser(1L, "alice", Role.ROLE_USER);
        user.setWalletBalance(new java.math.BigDecimal("100.00"));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        userService.updateWalletBalance("alice", new java.math.BigDecimal("40.00"), false);

        assertThat(user.getWalletBalance()).isEqualTo(new java.math.BigDecimal("60.00"));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateWalletBalance() - insufficient balance throws BadRequestException")
    void updateWalletBalance_insufficient_throwsException() {
        User user = buildUser(1L, "alice", Role.ROLE_USER);
        user.setWalletBalance(new java.math.BigDecimal("30.00"));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateWalletBalance("alice", new java.math.BigDecimal("50.00"), false))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient wallet balance");
    }
}
