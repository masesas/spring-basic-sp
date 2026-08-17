package com.masesas.exercises.demo1.dummyjsondto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Dimensi produk seperti dikirim dummyjson.com")
public class ProductDimensions {

    @Schema(description = "Lebar produk", example = "7.17")
    private BigDecimal width;
    @Schema(description = "Tinggi produk", example = "14.9")
    private BigDecimal height;
    @Schema(description = "Kedalaman produk", example = "0.8")
    private BigDecimal depth;
}
