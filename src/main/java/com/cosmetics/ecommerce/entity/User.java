package com.cosmetics.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 50)
    private String firstName;

    @Column(length = 50)
    private String lastName;

    @Column(length = 15)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    @Column(nullable = false)
    private Boolean isActive = true;

    // NEW FIELDS FOR APPROVAL SYSTEM
    @Column(nullable = false)
    private Boolean approved = false;  // Default: not approved

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus = AccountStatus.PENDING;  // PENDING, APPROVED, REJECTED

    private LocalDateTime approvedAt;

    private Long approvedBy;  // Admin user ID who approved

    @Column(length = 500)
    private String rejectionReason;  // Reason if rejected

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Address fields
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (role == Role.ADMIN) {
            approved = true;
            accountStatus = AccountStatus.APPROVED;
            approvedAt = LocalDateTime.now();
        }

    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();

        if (role == Role.ADMIN && !approved) {
            approved = true;
            accountStatus = AccountStatus.APPROVED;
            approvedAt = LocalDateTime.now();
        }
    }



    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive && approved;  // Account locked if not approved
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive && approved;  // Only enabled if active AND approved
    }

    // Role enum
    public enum Role {
        CUSTOMER,
        ADMIN
    }

    // AccountStatus enum
    public enum AccountStatus {
        PENDING,    // Waiting for admin approval
        APPROVED,   // Approved by admin
        REJECTED    // Rejected by admin
    }


}