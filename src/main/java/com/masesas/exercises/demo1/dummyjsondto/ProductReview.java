package com.masesas.exercises.demo1.dummyjsondto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Ulasan produk seperti dikirim dummyjson.com")
public class ProductReview {

    @Schema(description = "Penilaian pengulas, skala 1-5", example = "3")
    private Integer rating;
    @Schema(description = "Isi ulasan", example = "Would not recommend!")
    private String comment;
    @Schema(description = "Waktu ulasan dibuat", example = "2025-04-30T09:41:02.053Z")
    private String date;
    @Schema(description = "Nama pengulas", example = "Eleanor Collins")
    private String reviewerName;
    @Schema(description = "Email pengulas", example = "eleanor.collins@x.dummyjson.com")
    private String reviewerEmail;
}
