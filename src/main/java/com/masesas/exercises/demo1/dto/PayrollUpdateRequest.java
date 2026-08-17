package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Revisi nominal slip gaji")
public class PayrollUpdateRequest {

    @Schema(description = "Nomor versi baris yang sedang dipegang klien, dipakai untuk mendeteksi perubahan bersamaan", example = "0")
    private Long version;

    @PositiveOrZero(message = "gajiPokok tidak boleh negatif")
    @Schema(description = "Gaji pokok hasil revisi", example = "8500000.00")
    private BigDecimal gajiPokok;

    @PositiveOrZero(message = "tunjangan tidak boleh negatif")
    @Schema(description = "Total tunjangan hasil revisi", example = "1250000.00")
    private BigDecimal tunjangan;

    @PositiveOrZero(message = "potongan tidak boleh negatif")
    @Schema(description = "Total potongan hasil revisi", example = "450000.00")
    private BigDecimal potongan;
}
