package com.cosmetics.ecommerce.service;

import com.cosmetics.ecommerce.dto.BrandDTO;
import com.cosmetics.ecommerce.dto.BrandRequest;
import com.cosmetics.ecommerce.entity.Brand;
import com.cosmetics.ecommerce.repository.BrandRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
class BrandService {

    private final BrandRepository brandRepository;
    private final ModelMapper modelMapper;

    // Get all brands
    public List<BrandDTO> getAllBrands() {
        return brandRepository.findAll()
                .stream()
                .map(brand -> modelMapper.map(brand, BrandDTO.class))
                .collect(Collectors.toList());
    }

    // Get all active brands
    public List<BrandDTO> getAllActiveBrands() {
        return brandRepository.findByIsActiveTrue()
                .stream()
                .map(brand -> modelMapper.map(brand, BrandDTO.class))
                .collect(Collectors.toList());
    }

    // Get brand by ID
    public BrandDTO getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found with id: " + id));

        return modelMapper.map(brand, BrandDTO.class);
    }

    // Create brand
    public BrandDTO createBrand(BrandRequest request) {
        if (brandRepository.existsByName(request.getName())) {
            throw new RuntimeException("Brand with name '" + request.getName() + "' already exists");
        }

        Brand brand = new Brand();
        brand.setName(request.getName());
        brand.setDescription(request.getDescription());
        brand.setLogoUrl(request.getLogoUrl());

        Brand savedBrand = brandRepository.save(brand);
        return modelMapper.map(savedBrand, BrandDTO.class);
    }

    // Update brand
    public BrandDTO updateBrand(Long id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found with id: " + id));

        // Check if name is being changed and if new name already exists
        if (!brand.getName().equals(request.getName()) &&
                brandRepository.existsByName(request.getName())) {
            throw new RuntimeException("Brand with name '" + request.getName() + "' already exists");
        }

        brand.setName(request.getName());
        brand.setDescription(request.getDescription());
        brand.setLogoUrl(request.getLogoUrl());

        Brand updatedBrand = brandRepository.save(brand);
        return modelMapper.map(updatedBrand, BrandDTO.class);
    }

    // Delete brand (soft delete)
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found with id: " + id));

        brand.setIsActive(false);
        brandRepository.save(brand);
    }

    // Activate brand
    public void activateBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found with id: " + id));

        brand.setIsActive(true);
        brandRepository.save(brand);
    }
}
