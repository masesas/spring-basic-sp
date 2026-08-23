package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Produk pinjaman")
public class LoanProductRequest {

    @NotBlank
    @Size(max = 20)
    @Schema(description = "Kode produk, unik", example = "KTA")
    private String kode;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Nama produk", example = "Kredit Tanpa Agunan")
    private String nama;

    @NotNull
    @DecimalMin("0.0")
    @Schema(description = "Bunga per tahun dalam persen", example = "12.50")
    private BigDecimal bungaPersen;

    @NotNull
    @Positive
    @Schema(description = "Tenor terpendek dalam bulan", example = "6")
    private Integer tenorMin;

    @NotNull
    @Positive
    @Schema(description = "Tenor terpanjang dalam bulan", example = "36")
    private Integer tenorMax;

    @NotNull
    @DecimalMin("0.0")
    @Schema(description = "Nominal pinjaman terkecil", example = "5000000.00")
    private BigDecimal plafondMin;

    @NotNull
    @DecimalMin("0.0")
    @Schema(description = "Nominal pinjaman terbesar", example = "50000000.00")
    private BigDecimal plafondMax;

    @Schema(description = "Produk bisa dipakai mengajukan pinjaman, default true", example = "true")
    private Boolean aktif;
}
