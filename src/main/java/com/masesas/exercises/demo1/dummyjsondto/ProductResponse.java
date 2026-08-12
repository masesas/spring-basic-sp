package com.masesas.exercises.demo1.dummyjsondto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductResponse {

    private Integer id;
    private String title;
    private String description;
    private String category;
    private BigDecimal price;
    private BigDecimal discountPercentage;
    private BigDecimal rating;
    private Integer stock;
    private List<String> tags;
    private String brand;
    private String sku;
    private BigDecimal weight;
    private ProductDimensions dimensions;
    private String warrantyInformation;
    private String shippingInformation;
    private String availabilityStatus;
    private List<ProductReview> reviews;
    private String returnPolicy;
    private Integer minimumOrderQuantity;
    private ProductMeta meta;
    private List<String> images;
    private String thumbnail;
}
