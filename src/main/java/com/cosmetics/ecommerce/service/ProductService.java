package com.cosmetics.ecommerce.service;

import com.cosmetics.ecommerce.dto.*;
import com.cosmetics.ecommerce.entity.Brand;
import com.cosmetics.ecommerce.entity.Category;
import com.cosmetics.ecommerce.entity.Product;
import com.cosmetics.ecommerce.repository.BrandRepository;
import com.cosmetics.ecommerce.repository.CategoryRepository;
import com.cosmetics.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ModelMapper modelMapper;
    private final EmailService emailService;

    // Get all products with pagination
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable)
                .map(this::convertToDTO);
    }

    // Search products with filters
    public Page<ProductDTO> searchProducts(ProductSearchRequest request) {
        Pageable pageable = createPageable(request);

        // Search by keyword
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            return productRepository.searchByName(request.getKeyword(), pageable)
                    .map(this::convertToDTO);
        }

        // Filter by category and brand
        if (request.getCategoryId() != null && request.getBrandId() != null) {
            return productRepository.findByCategoryIdAndBrandIdAndIsActiveTrue(
                            request.getCategoryId(), request.getBrandId(), pageable)
                    .map(this::convertToDTO);
        }

        // Filter by category only
        if (request.getCategoryId() != null) {
            return productRepository.findByCategoryIdAndIsActiveTrue(
                            request.getCategoryId(), pageable)
                    .map(this::convertToDTO);
        }

        // Filter by brand only
        if (request.getBrandId() != null) {
            return productRepository.findByBrandIdAndIsActiveTrue(
                            request.getBrandId(), pageable)
                    .map(this::convertToDTO);
        }

        // Filter by price range
        if (request.getMinPrice() != null && request.getMaxPrice() != null) {
            return productRepository.findByPriceRange(
                            request.getMinPrice(), request.getMaxPrice(), pageable)
                    .map(this::convertToDTO);
        }

        // Default: return all active products
        return getAllProducts(pageable);
    }

    // Get product by ID
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Increment view count
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);

        return convertToDTO(product);
    }

    // Get featured products
    public List<ProductDTO> getFeaturedProducts() {
        Pageable pageable = PageRequest.of(0, 8);
        return productRepository.findByIsFeaturedTrueAndIsActiveTrue(pageable)
                .getContent()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Get top selling products
    public List<ProductDTO> getTopSellingProducts() {
        return productRepository.findTop10ByIsActiveTrueOrderBySoldCountDesc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Get products by category
    public Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable)
                .map(this::convertToDTO);
    }

    // Get products by brand
    public Page<ProductDTO> getProductsByBrand(Long brandId, Pageable pageable) {
        return productRepository.findByBrandIdAndIsActiveTrue(brandId, pageable)
                .map(this::convertToDTO);
    }

    // Create product (Admin only)
    public ProductDTO createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new RuntimeException("Brand not found"));

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setBrand(brand);
        product.setSize(request.getSize());
        product.setWeight(request.getWeight());
        product.setIngredients(request.getIngredients());
        product.setIsFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false);

        Product savedProduct = productRepository.save(product);
        return convertToDTO(savedProduct);
    }

    // Update product (Admin only)
    public ProductDTO updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new RuntimeException("Brand not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setBrand(brand);
        product.setSize(request.getSize());
        product.setWeight(request.getWeight());
        product.setIngredients(request.getIngredients());

        if (request.getIsFeatured() != null) {
            product.setIsFeatured(request.getIsFeatured());
        }

        Product updatedProduct = productRepository.save(product);

        if (product.getStockQuantity() < 10) {
            emailService.sendLowStockAlert(product.getName(), product.getStockQuantity());
        }

        return convertToDTO(updatedProduct);
    }

    // Delete product (soft delete - Admin only)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setIsActive(false);
        productRepository.save(product);
    }

    // Update stock quantity
    public void updateStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }

    // Check if product is in stock
    public boolean isInStock(Long productId, Integer requiredQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return product.getStockQuantity() >= requiredQuantity;
    }

    // Get low stock products (Admin only)
    public List<ProductDTO> getLowStockProducts(Integer threshold) {
        return productRepository.findLowStockProducts(threshold)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Helper method to create Pageable with sorting
    private Pageable createPageable(ProductSearchRequest request) {
        Sort sort;

        // Determine sort direction
        Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortOrder())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // Determine sort field
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "createdAt";
        sort = Sort.by(direction, sortBy);

        // Create pageable
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 12;

        return PageRequest.of(page, size, sort);
    }

    // Convert Product entity to DTO
    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = modelMapper.map(product, ProductDTO.class);

        if (product.getCategory() != null) {
            dto.setCategory(modelMapper.map(product.getCategory(), CategoryDTO.class));
        }

        if (product.getBrand() != null) {
            dto.setBrand(modelMapper.map(product.getBrand(), BrandDTO.class));
        }

        return dto;
    }
}