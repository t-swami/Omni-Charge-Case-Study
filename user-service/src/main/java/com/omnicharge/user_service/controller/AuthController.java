package com.omnicharge.user_service.controller;

import com.omnicharge.user_service.dto.*;
import com.omnicharge.user_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	private UserService userService;

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		AuthResponse response = userService.register(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/register-admin")
	public ResponseEntity<AuthResponse> registerAdmin(@Valid @RequestBody AdminRegisterRequest request) {
		AuthResponse response = userService.registerAdmin(request);
		return ResponseEntity.ok(response);
	}

	/** User login endpoint — rejects admin accounts with "Invalid credentials". */
	@PostMapping("/user/login")
	public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest request) {
		AuthResponse response = userService.loginUser(request);
		return ResponseEntity.ok(response);
	}

	/** Admin login endpoint — rejects non-admin accounts with "Invalid credentials". */
	@PostMapping("/admin/login")
	public ResponseEntity<AuthResponse> loginAdmin(@Valid @RequestBody LoginRequest request) {
		AuthResponse response = userService.loginAdmin(request);
		return ResponseEntity.ok(response);
	}
}