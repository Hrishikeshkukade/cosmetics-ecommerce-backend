package com.cosmetics.ecommerce.service;

import com.cosmetics.ecommerce.dto.UserDTO;
import com.cosmetics.ecommerce.entity.User;
import com.cosmetics.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserApprovalService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    public List<UserDTO> getPendingUsers() {
        List<User> pendingUsers = userRepository.findByAccountStatus(User.AccountStatus.PENDING);
        return pendingUsers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .filter(u -> u.getRole() != User.Role.ADMIN)  // Don't show admin users
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getAccountStatus() == User.AccountStatus.APPROVED) {
            throw new RuntimeException("User is already approved");
        }

        // Get current admin
        User admin = getCurrentAdmin();

        // Approve user
        user.setApproved(true);
        user.setAccountStatus(User.AccountStatus.APPROVED);
        user.setApprovedAt(LocalDateTime.now());
        user.setApprovedBy(admin.getId());
        user.setRejectionReason(null);

        User savedUser = userRepository.save(user);

        // Send approval email
        emailService.sendAccountApprovedEmail(user);

        return convertToDTO(savedUser);
    }

    public UserDTO rejectUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getAccountStatus() == User.AccountStatus.REJECTED) {
            throw new RuntimeException("User is already rejected");
        }

        // Reject user
        user.setApproved(false);
        user.setAccountStatus(User.AccountStatus.REJECTED);
        user.setRejectionReason(reason);
        user.setApprovedAt(null);
        user.setApprovedBy(null);

        User savedUser = userRepository.save(user);

        // Send rejection email
        emailService.sendAccountRejectedEmail(user, reason);

        return convertToDTO(savedUser);
    }

    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userRepository.count();
        long pendingUsers = userRepository.countByAccountStatus(User.AccountStatus.PENDING);
        long approvedUsers = userRepository.countByAccountStatus(User.AccountStatus.APPROVED);
        long rejectedUsers = userRepository.countByAccountStatus(User.AccountStatus.REJECTED);

        stats.put("total", totalUsers);
        stats.put("pending", pendingUsers);
        stats.put("approved", approvedUsers);
        stats.put("rejected", rejectedUsers);

        return stats;
    }

    private User getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
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
        dto.setAddress(user.getAddress());
        dto.setCity(user.getCity());
        dto.setState(user.getState());
        dto.setZipCode(user.getZipCode());
        dto.setCountry(user.getCountry());
        dto.setApproved(user.getApproved());
        dto.setAccountStatus(user.getAccountStatus().name());
        dto.setApprovedAt(user.getApprovedAt());
        dto.setRejectionReason(user.getRejectionReason());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}