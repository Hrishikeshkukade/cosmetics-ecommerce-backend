package com.cosmetics.ecommerce.controller;

import com.cosmetics.ecommerce.dto.CreateAdminRequest;
import com.cosmetics.ecommerce.dto.UserDTO;
import com.cosmetics.ecommerce.service.AdminManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/admins")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllAdmins() {
        return ResponseEntity.ok(adminManagementService.getAllAdmins());
    }

    @PostMapping
    public ResponseEntity<UserDTO> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminManagementService.createAdmin(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateAdmin(
            @PathVariable Long id,
            @Valid @RequestBody CreateAdminRequest request
    ) {
        return ResponseEntity.ok(adminManagementService.updateAdmin(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        adminManagementService.deleteAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
