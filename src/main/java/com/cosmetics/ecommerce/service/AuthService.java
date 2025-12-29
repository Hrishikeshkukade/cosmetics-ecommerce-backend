package com.cosmetics.ecommerce.service;

import com.cosmetics.ecommerce.dto.*;
import com.cosmetics.ecommerce.entity.User;
import com.cosmetics.ecommerce.repository.UserRepository;
import com.cosmetics.ecommerce.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final ModelMapper modelMapper;
    private final EmailService emailService;

    // Register new user
    public AuthResponse register(RegisterRequest request) {
        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(User.Role.CUSTOMER);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        emailService.sendWelcomeEmail(savedUser);

        // Generate JWT token
        String token = jwtUtil.generateToken(savedUser);

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    // Login user
    public AuthResponse login(LoginRequest request) {
        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // Get user details
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate JWT token
        String token = jwtUtil.generateToken(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    // Get current user profile
    public UserDTO getCurrentUserProfile() {
        User user = getCurrentUser();
        return modelMapper.map(user, UserDTO.class);
    }

    // Update user profile
    public UserDTO updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setCity(request.getCity());
        user.setState(request.getState());
        user.setZipCode(request.getZipCode());
        user.setCountry(request.getCountry());

        User updatedUser = userRepository.save(user);
        return modelMapper.map(updatedUser, UserDTO.class);
    }

    // Change password
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // Helper: Get current authenticated user
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}