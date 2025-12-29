package com.cosmetics.ecommerce.service;

import com.cosmetics.ecommerce.dto.*;
import com.cosmetics.ecommerce.entity.Order;
import com.cosmetics.ecommerce.entity.Product;
import com.cosmetics.ecommerce.entity.User;
import com.cosmetics.ecommerce.repository.OrderRepository;
import com.cosmetics.ecommerce.repository.ProductRepository;
import com.cosmetics.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // Create new order
    public OrderDTO createOrder(CreateOrderRequest request) {
        // Get current authenticated user
        User user = getCurrentUser();

        // Create order
        Order order = new Order();
        order.setUser(user);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCity(request.getShippingCity());
        order.setShippingState(request.getShippingState());
        order.setShippingZipCode(request.getShippingZipCode());
        order.setShippingCountry(request.getShippingCountry());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setNotes(request.getNotes());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);

        // Create order items and calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<com.cosmetics.ecommerce.entity.OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // Check stock availability
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }

            // Create order item
            com.cosmetics.ecommerce.entity.OrderItem orderItem = new com.cosmetics.ecommerce.entity.OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getEffectivePrice());

            BigDecimal subtotal = product.getEffectivePrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            orderItem.setSubtotal(subtotal);

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(subtotal);

            // Update product stock
            product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
            product.setSoldCount(product.getSoldCount() + itemRequest.getQuantity());
            productRepository.save(product);
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        emailService.sendOrderConfirmationEmail(user, savedOrder);

        return convertToDTO(savedOrder);
    }

    // Get all orders for current user
    public Page<OrderDTO> getUserOrders(Pageable pageable) {
        User user = getCurrentUser();
        return orderRepository.findByUserId(user.getId(), pageable)
                .map(this::convertToDTO);
    }

    // Get order by ID
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        // Check if user owns this order or is admin
        User currentUser = getCurrentUser();
        if (!order.getUser().getId().equals(currentUser.getId()) &&
                !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Access denied");
        }

        return convertToDTO(order);
    }

    // Get order by order number
    public OrderDTO getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found with order number: " + orderNumber));

        // Check if user owns this order or is admin
        User currentUser = getCurrentUser();
        if (!order.getUser().getId().equals(currentUser.getId()) &&
                !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Access denied");
        }

        return convertToDTO(order);
    }

    // Get all orders (Admin only)
    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    // Get orders by status (Admin only)
    public Page<OrderDTO> getOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable)
                .map(this::convertToDTO);
    }

    // Update order status (Admin only)
    public OrderDTO updateOrderStatus(Long id, Order.OrderStatus newStatus) {

        // Admin performing the action
        User admin = getCurrentUser();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        // 1️⃣ Store previous status BEFORE updating
        Order.OrderStatus previousStatus = order.getStatus();

        // 2️⃣ Update order status
        order.setStatus(newStatus);

        // 3️⃣ If order is delivered, mark payment as paid (COD)
        if (newStatus == Order.OrderStatus.DELIVERED &&
                order.getPaymentMethod() == Order.PaymentMethod.CASH_ON_DELIVERY) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
        }

        // 4️⃣ Save order
        Order updatedOrder = orderRepository.save(order);

        // 5️⃣ Send email to CUSTOMER (not admin)
        User customer = order.getUser();

        emailService.sendOrderStatusUpdateEmail(
                customer,
                updatedOrder,
                previousStatus.name()
        );

        return convertToDTO(updatedOrder);
    }


    // Cancel order
    public OrderDTO cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        // Check if user owns this order
        User currentUser = getCurrentUser();
        if (!order.getUser().getId().equals(currentUser.getId()) &&
                !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Access denied");
        }

        // Only allow cancellation if order is pending or confirmed
        if (order.getStatus() != Order.OrderStatus.PENDING &&
                order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new RuntimeException("Cannot cancel order in " + order.getStatus() + " status");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);

        // Restore product stock
        for (com.cosmetics.ecommerce.entity.OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            product.setSoldCount(product.getSoldCount() - item.getQuantity());
            productRepository.save(product);
        }

        Order cancelledOrder = orderRepository.save(order);
        return convertToDTO(cancelledOrder);
    }

    // Get order statistics (Admin only)
    public OrderStatsDTO getOrderStatistics() {
        OrderStatsDTO stats = new OrderStatsDTO();

        stats.setTotalOrders((long) orderRepository.findAll().size());
        stats.setPendingOrders(orderRepository.countByStatus(Order.OrderStatus.PENDING));
        stats.setConfirmedOrders(orderRepository.countByStatus(Order.OrderStatus.CONFIRMED));
        stats.setShippedOrders(orderRepository.countByStatus(Order.OrderStatus.SHIPPED));
        stats.setDeliveredOrders(orderRepository.countByStatus(Order.OrderStatus.DELIVERED));
        stats.setCancelledOrders(orderRepository.countByStatus(Order.OrderStatus.CANCELLED));

        Double totalRevenue = orderRepository.getTotalRevenue();
        stats.setTotalRevenue(totalRevenue != null ? totalRevenue : 0.0);

        // Calculate today's revenue
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        List<Order> todayOrders = orderRepository.findOrdersByDateRange(startOfDay, endOfDay);
        double todayRevenue = todayOrders.stream()
                .filter(o -> o.getPaymentStatus() == Order.PaymentStatus.PAID)
                .mapToDouble(o -> o.getTotalAmount().doubleValue())
                .sum();
        stats.setTodayRevenue(todayRevenue);

        // Calculate this month's revenue
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime endOfMonth = LocalDateTime.now();
        List<Order> monthOrders = orderRepository.findOrdersByDateRange(startOfMonth, endOfMonth);
        double monthRevenue = monthOrders.stream()
                .filter(o -> o.getPaymentStatus() == Order.PaymentStatus.PAID)
                .mapToDouble(o -> o.getTotalAmount().doubleValue())
                .sum();
        stats.setMonthRevenue(monthRevenue);

        return stats;
    }

    // Get recent orders (Admin only)
    public List<OrderSummaryDTO> getRecentOrders() {
        return orderRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    // Helper: Get current authenticated user
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Helper: Convert Order to OrderDTO
    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setUserId(order.getUser().getId());
        dto.setUsername(order.getUser().getUsername());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod().name());
        dto.setPaymentStatus(order.getPaymentStatus().name());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setShippingCity(order.getShippingCity());
        dto.setShippingState(order.getShippingState());
        dto.setShippingZipCode(order.getShippingZipCode());
        dto.setShippingCountry(order.getShippingCountry());
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerPhone(order.getCustomerPhone());
        dto.setCustomerEmail(order.getCustomerEmail());
        dto.setNotes(order.getNotes());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        List<OrderItemDTO> itemDTOs = order.getOrderItems().stream()
                .map(this::convertItemToDTO)
                .collect(Collectors.toList());
        dto.setOrderItems(itemDTOs);

        return dto;
    }

    // Helper: Convert OrderItem to OrderItemDTO
    private OrderItemDTO convertItemToDTO(com.cosmetics.ecommerce.entity.OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setProductImage(item.getProduct().getImageUrl());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }

    // Helper: Convert Order to OrderSummaryDTO
    private OrderSummaryDTO convertToSummaryDTO(Order order) {
        OrderSummaryDTO dto = new OrderSummaryDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus().name());
        dto.setItemCount(order.getOrderItems().size());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }
}