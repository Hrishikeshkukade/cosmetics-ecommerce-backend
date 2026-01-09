package com.cosmetics.ecommerce.security;

import com.cosmetics.ecommerce.entity.User;
import com.cosmetics.ecommerce.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class UserApprovalFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if user is authenticated
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {

            String username = authentication.getName();

            // Check if this is a protected endpoint
            String requestPath = request.getRequestURI();
            if (requiresApproval(requestPath)) {
                User user = userRepository.findByUsername(username).orElse(null);

                if (user != null) {
                    // Allow ADMIN users always
                    if (user.getRole() == User.Role.ADMIN) {
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // Check if CUSTOMER user is approved
                    if (!user.getApproved() || user.getAccountStatus() != User.AccountStatus.APPROVED) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\": \"Your account is pending approval. Please wait for admin confirmation.\"}");
                        return;
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresApproval(String path) {
        // Paths that require approved status
        return path.startsWith("/api/orders") ||
                path.startsWith("/api/cart") ||
                path.startsWith("/api/checkout");
    }
}
