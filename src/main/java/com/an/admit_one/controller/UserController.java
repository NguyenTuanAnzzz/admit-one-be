package com.an.admit_one.controller;

import com.an.admit_one.dto.request.UpdateMeRequest;
import com.an.admit_one.dto.response.MeResponse;
import com.an.admit_one.entity.User;
import com.an.admit_one.security.UserPrincipal;
import com.an.admit_one.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}