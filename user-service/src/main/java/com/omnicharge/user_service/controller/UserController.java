package com.omnicharge.user_service.controller;

import com.omnicharge.user_service.dto.ChangePasswordRequest;
import com.omnicharge.user_service.dto.UserDto;
import com.omnicharge.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Get own profile - any authenticated user
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getProfile(Authentication authentication) {
        UserDto userDto = userService.getUserProfile(authentication.getName());
        return ResponseEntity.ok(userDto);
    }

    // Get all users - ROLE_ADMIN only
    @GetMapping("/all")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // Promote user to admin - ROLE_ADMIN only
    @PutMapping("/promote/{userId}")
    public ResponseEntity<UserDto> promoteToAdmin(@PathVariable Long userId) {
        UserDto userDto = userService.promoteToAdmin(userId);
        return ResponseEntity.ok(userDto);
    }

    // Change own password - any authenticated user
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request) {
        String message = userService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    // Wallet endpoints
    @GetMapping("/profile/wallet")
    public ResponseEntity<java.math.BigDecimal> getWalletBalance(Authentication authentication) {
        return ResponseEntity.ok(userService.getWalletBalance(authentication.getName()));
    }

    @PostMapping("/profile/wallet/update")
    public ResponseEntity<Map<String, String>> updateWalletBalance(
            Authentication authentication,
            @RequestParam java.math.BigDecimal amount,
            @RequestParam boolean isTopUp) {
        userService.updateWalletBalance(authentication.getName(), amount, isTopUp);
        String action = isTopUp ? "added to" : "deducted from";
        return ResponseEntity.ok(Map.of("message", "Amount ₹" + amount + " " + action + " wallet successfully"));
    }
}
