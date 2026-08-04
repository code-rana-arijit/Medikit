package com.medikit.user.controller;

import com.medikit.common.security.UserContext;
import com.medikit.user.dto.AddressRequest;
import com.medikit.user.dto.AddressResponse;
import com.medikit.user.dto.UpdateProfileRequest;
import com.medikit.user.dto.UserResponse;
import com.medikit.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(userService.getProfile(UUID.fromString(UserContext.currentUserId())));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(UUID.fromString(UserContext.currentUserId()), request));
    }

    @GetMapping("/me/addresses")
    public ResponseEntity<List<AddressResponse>> addresses() {
        return ResponseEntity.ok(userService.getAddresses(UUID.fromString(UserContext.currentUserId())));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<AddressResponse> addAddress(@Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.addAddress(UUID.fromString(UserContext.currentUserId()), request));
    }

    @PutMapping("/me/addresses/{addressId}/default")
    public ResponseEntity<AddressResponse> setDefault(@PathVariable UUID addressId) {
        return ResponseEntity.ok(
                userService.setDefaultAddress(UUID.fromString(UserContext.currentUserId()), addressId));
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<Map<String, String>> deleteAddress(@PathVariable UUID addressId) {
        userService.deleteAddress(UUID.fromString(UserContext.currentUserId()), addressId);
        return ResponseEntity.ok(Map.of("message", "Address deleted"));
    }
}
