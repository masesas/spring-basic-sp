package com.masesas.exercises.demo1.dummyjsondto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Hasil pencarian produk dummyjson.com berikut informasi pagingnya")
public class ProductSearchResponse {

    @Schema(description = "Produk yang cocok dengan kata kunci")
    private List<ProductResponse> products;
    @Schema(description = "Jumlah seluruh produk yang cocok", example = "194")
    private Integer total;
    @Schema(description = "Banyaknya produk yang dilewati", example = "0")
    private Integer skip;
    @Schema(description = "Batas jumlah produk per permintaan", example = "30")
    private Integer limit;
}
