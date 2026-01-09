package com.cosmetics.ecommerce.controller;

import com.cosmetics.ecommerce.dto.UserApprovalRequest;
import com.cosmetics.ecommerce.dto.UserDTO;
import com.cosmetics.ecommerce.service.UserApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@PreAuthorize("hasRole('ADMIN')")
public class UserApprovalController {

    private final UserApprovalService userApprovalService;

    @GetMapping("/pending")
    public ResponseEntity<List<UserDTO>> getPendingUsers() {
        return ResponseEntity.ok(userApprovalService.getPendingUsers());
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userApprovalService.getAllUsers());
    }

    @PostMapping("/{userId}/approve")
    public ResponseEntity<UserDTO> approveUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userApprovalService.approveUser(userId));
    }

    @PostMapping("/{userId}/reject")
    public ResponseEntity<UserDTO> rejectUser(
            @PathVariable Long userId,
            @RequestBody UserApprovalRequest request
    ) {
        return ResponseEntity.ok(userApprovalService.rejectUser(userId, request.getReason()));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats() {
        return ResponseEntity.ok(userApprovalService.getUserStatistics());
    }
}