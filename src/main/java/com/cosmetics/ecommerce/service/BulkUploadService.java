package com.cosmetics.ecommerce.service;

import com.cosmetics.ecommerce.entity.Brand;
import com.cosmetics.ecommerce.entity.Category;
import com.cosmetics.ecommerce.entity.Product;
import com.cosmetics.ecommerce.repository.BrandRepository;
import com.cosmetics.ecommerce.repository.CategoryRepository;
import com.cosmetics.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class BulkUploadService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    public Map<String, Object> uploadProducts(MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> success = new ArrayList<>();
        int rowNumber = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row
            boolean isFirstRow = true;

            for (Row row : sheet) {
                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }

                rowNumber++;

                try {
                    Product product = parseRowToProduct(row, rowNumber);
                    if (product != null) {
                        productRepository.save(product);
                        success.add("Row " + rowNumber + ": " + product.getName() + " - SUCCESS");
                    }
                } catch (Exception e) {
                    errors.add("Row " + rowNumber + ": " + e.getMessage());
                }
            }
        }

        result.put("totalRows", rowNumber);
        result.put("successCount", success.size());
        result.put("errorCount", errors.size());
        result.put("errors", errors);
        result.put("success", success);

        return result;
    }

    private Product parseRowToProduct(Row row, int rowNumber) {
        try {
            // Column mapping (adjust according to your Excel template)
            String name = getCellValueAsString(row.getCell(0));
            String description = getCellValueAsString(row.getCell(1));
            BigDecimal price = getCellValueAsBigDecimal(row.getCell(2));
            BigDecimal discountPrice = getCellValueAsBigDecimal(row.getCell(3));
            Integer stockQuantity = getCellValueAsInteger(row.getCell(4));
            String categoryName = getCellValueAsString(row.getCell(5));
            String brandName = getCellValueAsString(row.getCell(6));
            String imageUrl = getCellValueAsString(row.getCell(7));
            String size = getCellValueAsString(row.getCell(8));
            String weight = getCellValueAsString(row.getCell(9));
            String ingredients = getCellValueAsString(row.getCell(10));
            Boolean isFeatured = getCellValueAsBoolean(row.getCell(11));

            // Validate required fields
            if (name == null || name.trim().isEmpty()) {
                throw new RuntimeException("Product name is required");
            }
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Valid price is required");
            }
            if (stockQuantity == null || stockQuantity < 0) {
                throw new RuntimeException("Valid stock quantity is required");
            }
            if (categoryName == null || categoryName.trim().isEmpty()) {
                throw new RuntimeException("Category is required");
            }
            if (brandName == null || brandName.trim().isEmpty()) {
                throw new RuntimeException("Brand is required");
            }

            // Find or create category
            Category category = categoryRepository.findByName(categoryName)
                    .orElseThrow(() -> new RuntimeException("Category '" + categoryName + "' not found"));

            // Find or create brand
            Brand brand = brandRepository.findByName(brandName)
                    .orElseThrow(() -> new RuntimeException("Brand '" + brandName + "' not found"));

            // Create product
            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setDiscountPrice(discountPrice);
            product.setStockQuantity(stockQuantity);
            product.setCategory(category);
            product.setBrand(brand);
            product.setImageUrl(imageUrl);
            product.setSize(size);
            product.setWeight(weight);
            product.setIngredients(ingredients);
            product.setIsFeatured(isFeatured != null ? isFeatured : false);
            product.setIsActive(true);

            return product;
        } catch (Exception e) {
            throw new RuntimeException("Error parsing row: " + e.getMessage());
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING:
                try {
                    return new BigDecimal(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private Boolean getCellValueAsBoolean(Cell cell) {
        if (cell == null) return false;

        switch (cell.getCellType()) {
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case STRING:
                String value = cell.getStringCellValue().trim().toLowerCase();
                return value.equals("true") || value.equals("yes") || value.equals("1");
            case NUMERIC:
                return cell.getNumericCellValue() == 1;
            default:
                return false;
        }
    }

    // Generate Excel template
    public Workbook generateTemplate() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Products");

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Product Name*", "Description", "Price*", "Discount Price",
                "Stock Quantity*", "Category*", "Brand*", "Image URL",
                "Size", "Weight", "Ingredients", "Is Featured (true/false)"
        };

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        // Add sample data row
        Row sampleRow = sheet.createRow(1);
        sampleRow.createCell(0).setCellValue("Sample Lipstick");
        sampleRow.createCell(1).setCellValue("Beautiful red lipstick");
        sampleRow.createCell(2).setCellValue(599.99);
        sampleRow.createCell(3).setCellValue(499.99);
        sampleRow.createCell(4).setCellValue(100);
        sampleRow.createCell(5).setCellValue("Makeup");
        sampleRow.createCell(6).setCellValue("Maybelline");
        sampleRow.createCell(7).setCellValue("https://example.com/image.jpg");
        sampleRow.createCell(8).setCellValue("5ml");
        sampleRow.createCell(9).setCellValue("10g");
        sampleRow.createCell(10).setCellValue("Vitamin E, Aloe Vera");
        sampleRow.createCell(11).setCellValue("true");

        return workbook;
    }
}
