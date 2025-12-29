package com.cosmetics.ecommerce.controller;

import com.cosmetics.ecommerce.dto.*;
import com.cosmetics.ecommerce.entity.Order;
import com.cosmetics.ecommerce.service.*;
import com.cosmetics.ecommerce.service.AnalyticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
class AdminController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final OrderService orderService;
    private final AnalyticsService analyticsService;

    @PostMapping("/products")
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok("Product deleted successfully");
    }

    @GetMapping("/products/low-stock")
    public ResponseEntity<List<ProductDTO>> getLowStockProducts(
            @RequestParam(defaultValue = "10") Integer threshold
    ) {
        return ResponseEntity.ok(productService.getLowStockProducts(threshold));
    }

    // ========== CATEGORY MANAGEMENT ==========
    @PostMapping("/categories")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok("Category deleted successfully");
    }

    @PutMapping("/categories/{id}/activate")
    public ResponseEntity<String> activateCategory(@PathVariable Long id) {
        categoryService.activateCategory(id);
        return ResponseEntity.ok("Category activated successfully");
    }

    // ========== BRAND MANAGEMENT ==========
    @PostMapping("/brands")
    public ResponseEntity<BrandDTO> createBrand(@Valid @RequestBody BrandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brandService.createBrand(request));
    }

    @PutMapping("/brands/{id}")
    public ResponseEntity<BrandDTO> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody BrandRequest request
    ) {
        return ResponseEntity.ok(brandService.updateBrand(id, request));
    }

    @DeleteMapping("/brands/{id}")
    public ResponseEntity<String> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok("Brand deleted successfully");
    }

    @PutMapping("/brands/{id}/activate")
    public ResponseEntity<String> activateBrand(@PathVariable Long id) {
        brandService.activateBrand(id);
        return ResponseEntity.ok("Brand activated successfully");
    }

    // ========== ORDER MANAGEMENT ==========
    @GetMapping("/orders")
    public ResponseEntity<Page<OrderDTO>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    @GetMapping("/orders/status/{status}")
    public ResponseEntity<Page<OrderDTO>> getOrdersByStatus(
            @PathVariable Order.OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(orderService.getOrdersByStatus(status, pageable));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody UpdateOrderStatusRequest request
    ) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.getStatus()));
    }

    @GetMapping("/orders/recent")
    public ResponseEntity<List<OrderSummaryDTO>> getRecentOrders() {
        return ResponseEntity.ok(orderService.getRecentOrders());
    }

    // ========== DASHBOARD STATISTICS ==========
    @GetMapping("/stats/orders")
    public ResponseEntity<OrderStatsDTO> getOrderStatistics() {
        return ResponseEntity.ok(orderService.getOrderStatistics());
    }

    @GetMapping("/analytics/sales-trend")
        public ResponseEntity<Map<String, Object>> getSalesTrend(
                @RequestParam(defaultValue = "30") int days
        ) {
            return ResponseEntity.ok(analyticsService.getSalesTrend(days));
        }


        @GetMapping("/analytics/top-products")
        public ResponseEntity<List<Map<String, Object>>> getTopProducts(
                @RequestParam(defaultValue = "10") int limit
        ) {
            return ResponseEntity.ok(analyticsService.getTopProducts(limit));
        }

        /**
         * Get sales distribution by category
         */
        @GetMapping("/analytics/category-distribution")
        public ResponseEntity<Map<String, Integer>> getCategoryDistribution() {
            return ResponseEntity.ok(analyticsService.getCategoryDistribution());
        }

        /**
         * Get revenue summary for different time periods
         */
        @GetMapping("/analytics/revenue-summary")
        public ResponseEntity<Map<String, Object>> getRevenueSummary() {
            return ResponseEntity.ok(analyticsService.getRevenueSummary());
        }

        /**
         * Get brand performance data
         */
        @GetMapping("/analytics/brand-performance")
        public ResponseEntity<Map<String, Object>> getBrandPerformance() {
            return ResponseEntity.ok(analyticsService.getBrandPerformance());
        }

        /**
         * Get comprehensive dashboard statistics
         */
        @GetMapping("/analytics/dashboard-stats")
        public ResponseEntity<Map<String, Object>> getDashboardStats() {
            return ResponseEntity.ok(analyticsService.getDashboardStats());
        }

        /**
         * Get order status distribution
         */
        @GetMapping("/analytics/order-status-distribution")
        public ResponseEntity<Map<String, Long>> getOrderStatusDistribution() {
            return ResponseEntity.ok(analyticsService.getOrderStatusDistribution());
        }

        /**
         * Get monthly sales comparison
         */
        @GetMapping("/analytics/monthly-comparison")
        public ResponseEntity<Map<String, Object>> getMonthlySalesComparison(
                @RequestParam(defaultValue = "6") int months
        ) {
            return ResponseEntity.ok(analyticsService.getMonthlySalesComparison(months));
        }
    }
