package com.cosmetics.ecommerce.service;

import com.cosmetics.ecommerce.entity.Order;
import com.cosmetics.ecommerce.entity.Product;
import com.cosmetics.ecommerce.repository.OrderRepository;
import com.cosmetics.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;


    public Map<String, Object> getSalesTrend(int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        List<Order> orders = orderRepository.findOrdersByDateRange(startDate, endDate);

        // Group orders by date and sum the total amounts
        Map<String, Double> dailySales = orders.stream()
                .filter(order -> order.getPaymentStatus() == Order.PaymentStatus.PAID)
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().toLocalDate().toString(),
                        Collectors.summingDouble(order -> order.getTotalAmount().doubleValue())
                ));

        // Sort dates
        List<String> sortedDates = new ArrayList<>(dailySales.keySet());
        Collections.sort(sortedDates);

        List<Double> sortedSales = sortedDates.stream()
                .map(dailySales::get)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("dates", sortedDates);
        response.put("sales", sortedSales);

        return response;
    }

    /**
     * Get top selling products with revenue information
     * @param limit Number of top products to return
     * @return List of product information maps
     */
    public List<Map<String, Object>> getTopProducts(int limit) {
        List<Product> topProducts = productRepository
                .findTop10ByIsActiveTrueOrderBySoldCountDesc()
                .stream()
                .limit(limit)
                .collect(Collectors.toList());

        return topProducts.stream()
                .map(product -> {
                    Map<String, Object> productData = new HashMap<>();
                    productData.put("id", product.getId());
                    productData.put("name", product.getName());
                    productData.put("soldCount", product.getSoldCount());
                    productData.put("revenue",
                            product.getSoldCount() * product.getEffectivePrice().doubleValue());
                    productData.put("category",
                            product.getCategory() != null ? product.getCategory().getName() : "Unknown");
                    productData.put("brand",
                            product.getCategory() != null ? product.getBrand().getName() : "Unknown");
                    return productData;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get sales distribution by category
     * @return Map of category names to sold counts
     */
    public Map<String, Integer> getCategoryDistribution() {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .filter(product -> product.getCategory() != null)
                .filter(product -> product.getSoldCount() > 0) // Only include products with sales
                .collect(Collectors.groupingBy(
                        product -> product.getCategory().getName(),
                        Collectors.summingInt(Product::getSoldCount)
                ));
    }


    public Map<String, Object> getRevenueSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfWeek = now.minusDays(7);
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);

        List<Order> allOrders = orderRepository.findAll();

        // Calculate today's revenue
        double todayRevenue = calculateRevenue(allOrders, startOfDay);

        // Calculate this week's revenue
        double weekRevenue = calculateRevenue(allOrders, startOfWeek);

        // Calculate this month's revenue
        double monthRevenue = calculateRevenue(allOrders, startOfMonth);

        // Calculate total revenue
        double totalRevenue = calculateRevenue(allOrders, LocalDateTime.MIN);

        Map<String, Object> summary = new HashMap<>();
        summary.put("today", todayRevenue);
        summary.put("week", weekRevenue);
        summary.put("month", monthRevenue);
        summary.put("total", totalRevenue);

        return summary;
    }


    public Map<String, Object> getBrandPerformance() {
        List<Product> products = productRepository.findAll();

        Map<String, Map<String, Object>> brandData = products.stream()
                .filter(product -> product.getBrand() != null)
                .filter(product -> product.getSoldCount() > 0)
                .collect(Collectors.groupingBy(
                        product -> product.getBrand().getName(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                productList -> {
                                    Map<String, Object> data = new HashMap<>();
                                    int totalSold = productList.stream()
                                            .mapToInt(Product::getSoldCount)
                                            .sum();
                                    double totalRevenue = productList.stream()
                                            .mapToDouble(p -> p.getSoldCount() *
                                                    p.getEffectivePrice().doubleValue())
                                            .sum();
                                    data.put("totalSold", totalSold);
                                    data.put("totalRevenue", totalRevenue);
                                    data.put("productCount", productList.size());
                                    return data;
                                }
                        )
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("brands", brandData);

        return response;
    }

    /**
     * Get comprehensive dashboard statistics
     * @return Map containing various dashboard metrics
     */
    public Map<String, Object> getDashboardStats() {
        List<Order> allOrders = orderRepository.findAll();
        List<Product> allProducts = productRepository.findAll();

        // Order statistics
        long totalOrders = allOrders.size();
        long pendingOrders = allOrders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.PENDING)
                .count();
        long deliveredOrders = allOrders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED)
                .count();

        // Product statistics
        long totalProducts = allProducts.size();
        long lowStockProducts = allProducts.stream()
                .filter(p -> p.getStockQuantity() < 10)
                .count();
        long outOfStockProducts = allProducts.stream()
                .filter(p -> p.getStockQuantity() == 0)
                .count();

        // Revenue
        double totalRevenue = calculateRevenue(allOrders, LocalDateTime.MIN);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", totalOrders);
        stats.put("pendingOrders", pendingOrders);
        stats.put("deliveredOrders", deliveredOrders);
        stats.put("totalProducts", totalProducts);
        stats.put("lowStockProducts", lowStockProducts);
        stats.put("outOfStockProducts", outOfStockProducts);
        stats.put("totalRevenue", totalRevenue);

        return stats;
    }

    /**
     * Helper method to calculate revenue after a specific date
     * @param orders List of all orders
     * @param startDate Starting date for calculation
     * @return Total revenue
     */
    private double calculateRevenue(List<Order> orders, LocalDateTime startDate) {
        return orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startDate))
                .filter(order -> order.getPaymentStatus() == Order.PaymentStatus.PAID)
                .mapToDouble(order -> order.getTotalAmount().doubleValue())
                .sum();
    }

    /**
     * Get order status distribution
     * @return Map of order statuses to counts
     */
    public Map<String, Long> getOrderStatusDistribution() {
        List<Order> allOrders = orderRepository.findAll();

        return allOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getStatus().name(),
                        Collectors.counting()
                ));
    }

    /**
     * Get monthly sales comparison for last N months
     * @param months Number of months to compare
     * @return Map containing month labels and sales data
     */
    public Map<String, Object> getMonthlySalesComparison(int months) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(months);

        List<Order> orders = orderRepository.findOrdersByDateRange(startDate, endDate);

        Map<String, Double> monthlySales = orders.stream()
                .filter(order -> order.getPaymentStatus() == Order.PaymentStatus.PAID)
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().getYear() + "-" +
                                String.format("%02d", order.getCreatedAt().getMonthValue()),
                        Collectors.summingDouble(order -> order.getTotalAmount().doubleValue())
                ));

        List<String> sortedMonths = new ArrayList<>(monthlySales.keySet());
        Collections.sort(sortedMonths);

        List<Double> sortedSales = sortedMonths.stream()
                .map(monthlySales::get)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("months", sortedMonths);
        response.put("sales", sortedSales);

        return response;
    }
}
