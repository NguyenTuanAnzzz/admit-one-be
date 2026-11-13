package com.an.admit_one.controller;

import com.an.admit_one.dto.request.CreateUserRequest;
import com.an.admit_one.dto.request.UpdateMeRequest;
import com.an.admit_one.dto.response.MeResponse;
import com.an.admit_one.dto.response.UserResponse;
import com.an.admit_one.enums.UserRole;
import com.an.admit_one.security.UserPrincipal;
import com.an.admit_one.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        MeResponse meResponse = userService.getUser(principal);
        return ResponseEntity.status(200).body(meResponse);
    }

    @PutMapping("/me")
    public ResponseEntity<MeResponse> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateMeRequest request
    ) {
        MeResponse meResponse = userService.updateMe(principal, request);
        return ResponseEntity.ok(meResponse);
    }

    // Dashboard › Người dùng (admin): liệt kê + tạo/sửa tài khoản cả 3 role (ADMIN, STAFF, CUSTOMER)

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UserResponse>> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(userService.listUsers(role, q, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(201).body(userService.createUser(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMeRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }
}
