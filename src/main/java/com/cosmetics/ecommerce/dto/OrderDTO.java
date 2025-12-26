package com.cosmetics.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String username;
    private List<OrderItemDTO> orderItems;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
    private String paymentStatus;

    // Shipping details
    private String shippingAddress;
    private String shippingCity;
    private String shippingState;
    private String shippingZipCode;
    private String shippingCountry;
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
