package com.cosmetics.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String role;
    private String message;  // NEW: Message for user
    private Boolean approvalRequired = false;  // NEW: Indicates if approval is needed

    public AuthResponse(String token, Long id, String username, String email, String role) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.message = "Success";
        this.approvalRequired = false;
    }

    public AuthResponse(String token, Long id, String username, String email, String role, String message, Boolean approvalRequired) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.message = message;
        this.approvalRequired = approvalRequired;
    }
}