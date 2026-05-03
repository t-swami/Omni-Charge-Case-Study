package com.omnicharge.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.user_service.dto.*;
import com.omnicharge.user_service.exception.*;
import com.omnicharge.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController standalone MockMvc test.
 *
 * standaloneSetup() = zero Spring context, zero Security, no 401 ever.
 * GlobalExceptionHandler registered manually so custom exceptions map to correct HTTP codes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Web Layer Tests")
class AuthControllerTest {

	@InjectMocks
	private AuthController authController;

	@Mock
	private UserService userService;

	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(authController)
				.setControllerAdvice(new GlobalExceptionHandler()).build();
	}

	// ── POST /api/auth/register ───────────────────────────────────────────────

	@Test
	@DisplayName("POST /api/auth/register - 200 OK on successful registration")
	void register_returns200() throws Exception {
		RegisterRequest req = new RegisterRequest();
		req.setUsername("alice");
		req.setEmail("alice@test.com");
		req.setPassword("pass123");
		req.setFullName("Alice");
		req.setPhone("9876543210");

		AuthResponse resp = new AuthResponse("jwt.token", "alice", "ROLE_USER", "User registered successfully");

		when(userService.register(any(RegisterRequest.class))).thenReturn(resp);

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("jwt.token")).andExpect(jsonPath("$.username").value("alice"))
				.andExpect(jsonPath("$.role").value("ROLE_USER"))
				.andExpect(jsonPath("$.message").value("User registered successfully"));
	}

	@Test
	@DisplayName("POST /api/auth/register - duplicate username returns 409 Conflict")
	void register_duplicateUsername_returns409() throws Exception {
		RegisterRequest req = new RegisterRequest();
		req.setUsername("alice");
		req.setEmail("alice@test.com");
		req.setPassword("pass123");
		req.setFullName("Alice");
		req.setPhone("9876543210");

		when(userService.register(any(RegisterRequest.class)))
				.thenThrow(new UserAlreadyExistsException("Username already exists: alice"));

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Username already exists: alice"));
	}

	// ── POST /api/auth/user/login ─────────────────────────────────────────────

	@Test
	@DisplayName("POST /api/auth/user/login - 200 OK for ROLE_USER")
	void loginUser_returns200() throws Exception {
		LoginRequest req = new LoginRequest();
		req.setUsername("alice");
		req.setPassword("pass123");

		AuthResponse resp = new AuthResponse("jwt.login.token", "alice", "ROLE_USER", "Login successful");

		when(userService.loginUser(any(LoginRequest.class))).thenReturn(resp);

		mockMvc.perform(post("/api/auth/user/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("jwt.login.token"))
				.andExpect(jsonPath("$.role").value("ROLE_USER"))
				.andExpect(jsonPath("$.message").value("Login successful"));
	}

	@Test
	@DisplayName("POST /api/auth/user/login - admin tries user endpoint returns 401")
	void loginUser_adminAccount_returns401() throws Exception {
		LoginRequest req = new LoginRequest();
		req.setUsername("admin_user");
		req.setPassword("adminpass");

		when(userService.loginUser(any(LoginRequest.class)))
				.thenThrow(new InvalidCredentialsException("Invalid credentials"));

		mockMvc.perform(post("/api/auth/user/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid credentials"));
	}

	@Test
	@DisplayName("POST /api/auth/user/login - bad credentials returns 401")
	void loginUser_badCredentials_returns401() throws Exception {
		LoginRequest req = new LoginRequest();
		req.setUsername("alice");
		req.setPassword("wrongpass");

		when(userService.loginUser(any(LoginRequest.class)))
				.thenThrow(new InvalidCredentialsException("Invalid username or password"));

		mockMvc.perform(post("/api/auth/user/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid username or password"));
	}

	// ── POST /api/auth/admin/login ────────────────────────────────────────────

	@Test
	@DisplayName("POST /api/auth/admin/login - 200 OK for ROLE_ADMIN")
	void loginAdmin_returns200() throws Exception {
		LoginRequest req = new LoginRequest();
		req.setUsername("admin_user");
		req.setPassword("adminpass");

		AuthResponse resp = new AuthResponse("jwt.admin.token", "admin_user", "ROLE_ADMIN", "Login successful");

		when(userService.loginAdmin(any(LoginRequest.class))).thenReturn(resp);

		mockMvc.perform(post("/api/auth/admin/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("jwt.admin.token"))
				.andExpect(jsonPath("$.role").value("ROLE_ADMIN"))
				.andExpect(jsonPath("$.message").value("Login successful"));
	}

	@Test
	@DisplayName("POST /api/auth/admin/login - user tries admin endpoint returns 401")
	void loginAdmin_userAccount_returns401() throws Exception {
		LoginRequest req = new LoginRequest();
		req.setUsername("alice");
		req.setPassword("pass123");

		when(userService.loginAdmin(any(LoginRequest.class)))
				.thenThrow(new InvalidCredentialsException("Invalid credentials"));

		mockMvc.perform(post("/api/auth/admin/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid credentials"));
	}

	@Test
	@DisplayName("POST /api/auth/admin/login - bad credentials returns 401")
	void loginAdmin_badCredentials_returns401() throws Exception {
		LoginRequest req = new LoginRequest();
		req.setUsername("admin_user");
		req.setPassword("wrongpass");

		when(userService.loginAdmin(any(LoginRequest.class)))
				.thenThrow(new InvalidCredentialsException("Invalid username or password"));

		mockMvc.perform(post("/api/auth/admin/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid username or password"));
	}

	// ── POST /api/auth/register-admin ─────────────────────────────────────────

	@Test
	@DisplayName("POST /api/auth/register-admin - 200 OK with valid secret key")
	void registerAdmin_returns200() throws Exception {
		AdminRegisterRequest req = new AdminRegisterRequest();
		req.setUsername("adminuser");
		req.setEmail("admin@test.com");
		req.setPassword("adminpass");
		req.setFullName("Admin User");
		req.setPhone("9000000001");
		req.setAdminSecretKey("omniCharge008");

		AuthResponse resp = new AuthResponse("jwt.admin.token", "adminuser", "ROLE_ADMIN",
				"Admin registered successfully");

		when(userService.registerAdmin(any(AdminRegisterRequest.class))).thenReturn(resp);

		mockMvc.perform(post("/api/auth/register-admin").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("ROLE_ADMIN"))
				.andExpect(jsonPath("$.token").value("jwt.admin.token"));
	}

	@Test
	@DisplayName("POST /api/auth/register-admin - wrong secret key returns 401")
	void registerAdmin_wrongKey_returns401() throws Exception {
		AdminRegisterRequest req = new AdminRegisterRequest();
		req.setUsername("hacker");
		req.setEmail("hacker@test.com");
		req.setPassword("hackpass");
		req.setFullName("Hacker");
		req.setPhone("9000000002");
		req.setAdminSecretKey("wrong_key");

		when(userService.registerAdmin(any(AdminRegisterRequest.class)))
				.thenThrow(new InvalidCredentialsException("Invalid admin secret key"));

		mockMvc.perform(post("/api/auth/register-admin").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid admin secret key"));
	}

	// ── Validation ────────────────────────────────────────────────────────────

	@Test
	@DisplayName("POST /api/auth/register - invalid email returns 400")
	void register_invalidEmail_returns400() throws Exception {
		RegisterRequest req = new RegisterRequest();
		req.setUsername("alice");
		req.setEmail("wrong-email");
		req.setPassword("pass123");
		req.setFullName("Alice");
		req.setPhone("9876543210");

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isBadRequest());
	}
}