package com.cosmetics.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer stockQuantity;
    private String imageUrl;
    private CategoryDTO category;
    private BrandDTO brand;
    private String size;
    private String weight;
    private String ingredients;
    private Boolean isActive;
    private Boolean isFeatured;
    private Integer viewCount;
    private Integer soldCount;
}
