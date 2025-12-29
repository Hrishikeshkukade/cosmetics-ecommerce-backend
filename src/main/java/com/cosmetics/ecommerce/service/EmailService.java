package com.cosmetics.ecommerce.service;

import com.cosmetics.ecommerce.entity.Order;
import com.cosmetics.ecommerce.entity.User;
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

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

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
            message.setTo("admin@glowbeauty.com"); // Admin email
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
