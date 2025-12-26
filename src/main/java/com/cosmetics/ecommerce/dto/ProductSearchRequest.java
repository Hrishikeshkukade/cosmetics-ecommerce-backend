package com.cosmetics.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchRequest {
    private String keyword;
    private Long categoryId;
    private Long brandId;
    private Double minPrice;
    private Double maxPrice;
    private String sortBy = "createdAt";
    private String sortOrder = "desc";
    private Integer page = 0;
    private Integer size = 12;
}
