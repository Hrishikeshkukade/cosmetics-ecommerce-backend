package com.cosmetics.ecommerce.repository;

import com.cosmetics.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find by category
    Page<Product> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);

    // Find by brand
    Page<Product> findByBrandIdAndIsActiveTrue(Long brandId, Pageable pageable);

    // Find featured products
    Page<Product> findByIsFeaturedTrueAndIsActiveTrue(Pageable pageable);

    // Search by name
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.isActive = true")
    Page<Product> searchByName(@Param("keyword") String keyword, Pageable pageable);

    // Find by category and brand
    Page<Product> findByCategoryIdAndBrandIdAndIsActiveTrue(Long categoryId, Long brandId, Pageable pageable);

    // Find all active products
    Page<Product> findByIsActiveTrue(Pageable pageable);

    // Find products by price range
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.isActive = true")
    Page<Product> findByPriceRange(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice, Pageable pageable);

    // Find low stock products (for admin)
    @Query("SELECT p FROM Product p WHERE p.stockQuantity < :threshold AND p.isActive = true")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);

    // Get top selling products
    List<Product> findTop10ByIsActiveTrueOrderBySoldCountDesc();
}

