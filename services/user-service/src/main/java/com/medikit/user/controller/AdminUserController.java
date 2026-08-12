package com.medikit.user.controller;

import com.medikit.common.security.UserContext;
import com.medikit.common.web.PageResult;
import com.medikit.user.dto.RoleUpgradeRequest;
import com.medikit.user.dto.UserResponse;
import com.medikit.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<PageResult<UserResponse>> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.adminSearchUsers(q, page, size));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<UserResponse> setRole(
            @PathVariable UUID userId,
            @Valid @RequestBody RoleUpgradeRequest request) {
        UUID adminId = UUID.fromString(UserContext.currentUserId());
        return ResponseEntity.ok(userService.adminSetRole(adminId, userId, request.role()));
    }
}
