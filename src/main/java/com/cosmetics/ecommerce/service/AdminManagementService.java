package com.cosmetics.ecommerce.service;

import com.cosmetics.ecommerce.dto.CreateAdminRequest;
import com.cosmetics.ecommerce.dto.UserDTO;
import com.cosmetics.ecommerce.entity.User;
import com.cosmetics.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserDTO createAdmin(CreateAdminRequest request) {
        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Get current admin who is creating this new admin
        User creatingAdmin = getCurrentAdmin();

        // Create new admin user
        User newAdmin = new User();
        newAdmin.setUsername(request.getUsername());
        newAdmin.setEmail(request.getEmail());
        newAdmin.setPassword(passwordEncoder.encode(request.getPassword()));
        newAdmin.setFirstName(request.getFirstName());
        newAdmin.setLastName(request.getLastName());
        newAdmin.setPhoneNumber(request.getPhoneNumber());

        // Set as ADMIN role
        newAdmin.setRole(User.Role.ADMIN);

        // Auto-approve admin users
        newAdmin.setIsActive(true);
        newAdmin.setApproved(true);
        newAdmin.setAccountStatus(User.AccountStatus.APPROVED);
        newAdmin.setApprovedAt(LocalDateTime.now());
        newAdmin.setApprovedBy(creatingAdmin.getId());

        User savedAdmin = userRepository.save(newAdmin);

        // Send welcome email to new admin
        sendAdminWelcomeEmail(savedAdmin, request.getPassword());

        return convertToDTO(savedAdmin);
    }

    public List<UserDTO> getAllAdmins() {
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        return admins.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void deleteAdmin(Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Prevent deleting yourself
        User currentAdmin = getCurrentAdmin();
        if (admin.getId().equals(currentAdmin.getId())) {
            throw new RuntimeException("You cannot delete your own admin account");
        }

        // Check if this is the only admin
        long adminCount = userRepository.countByRole(User.Role.ADMIN);
        if (adminCount <= 1) {
            throw new RuntimeException("Cannot delete the last admin account");
        }

        if (admin.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("User is not an admin");
        }

        userRepository.delete(admin);
    }

    public UserDTO updateAdmin(Long adminId, CreateAdminRequest request) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (admin.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("User is not an admin");
        }

        // Update fields
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());
        admin.setPhoneNumber(request.getPhoneNumber());

        // Only update email if changed and not duplicate
        if (!admin.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            admin.setEmail(request.getEmail());
        }

        // Only update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updated = userRepository.save(admin);
        return convertToDTO(updated);
    }

    private User getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    private void sendAdminWelcomeEmail(User admin, String tempPassword) {
        try {
            String subject = "Welcome as Admin - GlowBeauty";
            String body = String.format("""
                Dear %s,
                
                You have been created as an administrator for GlowBeauty E-commerce Platform.
                
                Your login credentials:
                Username: %s
                Temporary Password: %s
                
                Please login and change your password immediately.
                
                Admin Panel: http://localhost:5173/login
                
                Best regards,
                GlowBeauty Team
                """, admin.getFirstName(), admin.getUsername(), tempPassword);

            // Use simple email for now (you can create HTML template later)
            emailService.sendSimpleEmail(admin.getEmail(), subject, body);
        } catch (Exception e) {
            System.err.println("Failed to send admin welcome email: " + e.getMessage());
        }
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole().name());
        dto.setIsActive(user.getIsActive());
        dto.setApproved(user.getApproved());
        dto.setAccountStatus(user.getAccountStatus().name());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
