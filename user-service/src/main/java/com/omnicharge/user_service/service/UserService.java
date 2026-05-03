 package com.omnicharge.user_service.service;

import com.omnicharge.user_service.dto.*;

import java.util.List;

public interface UserService {

	AuthResponse register(RegisterRequest request);

	AuthResponse registerAdmin(AdminRegisterRequest request);

	AuthResponse loginUser(LoginRequest request);

	AuthResponse loginAdmin(LoginRequest request);

	UserDto getUserProfile(String username);

	List<UserDto> getAllUsers();

	UserDto promoteToAdmin(Long userId);

	String changePassword(String username, ChangePasswordRequest request);

	java.math.BigDecimal getWalletBalance(String username);

	void updateWalletBalance(String username, java.math.BigDecimal amount, boolean isTopUp);
}