package com.cosmetics.ecommerce.service;

import com.cosmetics.ecommerce.entity.Order;
import com.cosmetics.ecommerce.entity.User;
import com.cosmetics.ecommerce.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final UserRepository userRepository;  // NEW: Add this

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${spring.application.name}")
    private String appName;

    @Async
    public void sendWelcomeEmail(User user) {
        try {
            Context context = new Context();
            context.setVariable("name", user.getFirstName());
            context.setVariable("username", user.getUsername());
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("welcome-email", context);

            sendHtmlEmail(
                    user.getEmail(),
                    "Welcome to " + appName + "!",
                    htmlContent
            );
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
    }

    @Async
    public void sendOrderConfirmationEmail(User user, Order order) {
        try {
            Context context = new Context();
            context.setVariable("name", user.getFirstName());
            context.setVariable("orderNumber", order.getOrderNumber());
            context.setVariable("orderDate", order.getCreatedAt());
            context.setVariable("totalAmount", order.getTotalAmount());
            context.setVariable("items", order.getOrderItems());
            context.setVariable("shippingAddress", order.getShippingAddress() + ", " +
                    order.getShippingCity() + ", " + order.getShippingState());

            String htmlContent = templateEngine.process("order-confirmation", context);

            sendHtmlEmail(
                    user.getEmail(),
                    "Order Confirmation - " + order.getOrderNumber(),
                    htmlContent
            );
        } catch (Exception e) {
            System.err.println("Failed to send order confirmation email: " + e.getMessage());
        }
    }

    @Async
    public void sendOrderStatusUpdateEmail(User user, Order order, String previousStatus) {
        try {
            Context context = new Context();
            context.setVariable("name", user.getFirstName());
            context.setVariable("orderNumber", order.getOrderNumber());
            context.setVariable("status", order.getStatus());
            context.setVariable("previousStatus", previousStatus);

            String htmlContent = templateEngine.process("order-status-update", context);

            sendHtmlEmail(
                    user.getEmail(),
                    "Order Update - " + order.getOrderNumber(),
                    htmlContent
            );
        } catch (Exception e) {
            System.err.println("Failed to send order status update email: " + e.getMessage());
        }
    }

    @Async
    public void sendOrderDeliveredEmail(User user, Order order) {
        try {
            Context context = new Context();
            context.setVariable("name", user.getFirstName());
            context.setVariable("orderNumber", order.getOrderNumber());
            context.setVariable("deliveryDate", order.getUpdatedAt());

            String htmlContent = templateEngine.process("order-delivered", context);

            sendHtmlEmail(
                    user.getEmail(),
                    "Your Order Has Been Delivered - " + order.getOrderNumber(),
                    htmlContent
            );
        } catch (Exception e) {
            System.err.println("Failed to send order delivered email: " + e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String email, String resetToken) {
        try {
            Context context = new Context();
            context.setVariable("resetLink", "http://localhost:3000/reset-password?token=" + resetToken);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("password-reset", context);

            sendHtmlEmail(
                    email,
                    "Password Reset Request",
                    htmlContent
            );
        } catch (Exception e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
        }
    }

    @Async
    public void sendLowStockAlert(String productName, int currentStock) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo("admin@glowbeauty.com");
            message.setSubject("Low Stock Alert - " + productName);
            message.setText(
                    "Warning: Product '" + productName + "' is running low on stock.\n\n" +
                            "Current Stock: " + currentStock + " units\n\n" +
                            "Please restock soon to avoid stockouts."
            );

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send low stock alert: " + e.getMessage());
        }
    }

    // NEW: Account Pending Email
    @Async
    public void sendAccountPendingEmail(User user) {
        try {
            System.out.println("=== Sending Pending Email ===");
            System.out.println("To: " + user.getEmail());
            System.out.println("Name: " + user.getFirstName());

            Context context = new Context();
            context.setVariable("name", user.getFirstName());
            context.setVariable("username", user.getUsername());
            context.setVariable("email", user.getEmail());
            context.setVariable("createdAt", user.getCreatedAt());

            String htmlContent = templateEngine.process("account-pending", context);

            sendHtmlEmail(
                    user.getEmail(),
                    "Account Registration - Pending Approval",
                    htmlContent
            );

            System.out.println("✅ Pending email sent successfully!");
        } catch (Exception e) {
            System.err.println("❌ Failed to send account pending email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void sendNewRegistrationNotificationToAdmin(User user) {
        try {
            System.out.println("=== Sending Admin Notification ===");

            List<User> admins = userRepository.findByRole(User.Role.ADMIN);
            System.out.println("Found " + admins.size() + " admin(s)");

            for (User admin : admins) {
                System.out.println("Notifying admin: " + admin.getEmail());

                Context context = new Context();
                context.setVariable("username", user.getUsername());
                context.setVariable("email", user.getEmail());
                context.setVariable("fullName", user.getFirstName() + " " + user.getLastName());
                context.setVariable("phone", user.getPhoneNumber());
                context.setVariable("registrationDate", user.getCreatedAt());
                context.setVariable("userId", user.getId());
                context.setVariable("adminPanelUrl", "http://localhost:5173");

                String htmlContent = templateEngine.process("new-registration-admin", context);

                sendHtmlEmail(
                        admin.getEmail(),
                        "New User Registration - Approval Required",
                        htmlContent
                );

                System.out.println("✅ Admin notification sent to: " + admin.getEmail());
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to send admin notification email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // NEW: Account Approved Email
    @Async
    public void sendAccountApprovedEmail(User user) {
        try {
            Context context = new Context();
            context.setVariable("name", user.getFirstName());
            context.setVariable("username", user.getUsername());
            context.setVariable("email", user.getEmail());
            context.setVariable("approvedAt", user.getApprovedAt());
            context.setVariable("loginUrl", "http://localhost:5173/login");

            String htmlContent = templateEngine.process("account-approved", context);

            sendHtmlEmail(
                    user.getEmail(),
                    "Account Approved - Welcome to GlowBeauty!",
                    htmlContent
            );
        } catch (Exception e) {
            System.err.println("Failed to send account approved email: " + e.getMessage());
        }
    }

    // NEW: Account Rejected Email
    @Async
    public void sendAccountRejectedEmail(User user, String reason) {
        try {
            Context context = new Context();
            context.setVariable("name", user.getFirstName());
            context.setVariable("username", user.getUsername());
            context.setVariable("reason", reason != null ? reason : "No specific reason provided");

            String htmlContent = templateEngine.process("account-rejected", context);

            sendHtmlEmail(
                    user.getEmail(),
                    "Account Registration - Update",
                    htmlContent
            );
        } catch (Exception e) {
            System.err.println("Failed to send account rejected email: " + e.getMessage());
        }
    }

    // NEW: Admin Notification Email
//    @Async
//    public void sendNewRegistrationNotificationToAdmin(User user) {
//        try {
//            List<User> admins = userRepository.findByRole(User.Role.ADMIN);
//
//            for (User admin : admins) {
//                Context context = new Context();
//                context.setVariable("username", user.getUsername());
//                context.setVariable("email", user.getEmail());
//                context.setVariable("fullName", user.getFirstName() + " " + user.getLastName());
//                context.setVariable("phone", user.getPhoneNumber());
//                context.setVariable("registrationDate", user.getCreatedAt());
//                context.setVariable("userId", user.getId());
//                context.setVariable("adminPanelUrl", "http://localhost:5173/admin/users");
//
//                String htmlContent = templateEngine.process("new-registration-admin", context);
//
//                sendHtmlEmail(
//                        admin.getEmail(),
//                        "New User Registration - Approval Required",
//                        htmlContent
//                );
//            }
//        } catch (Exception e) {
//            System.err.println("Failed to send admin notification email: " + e.getMessage());
//        }
//    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}