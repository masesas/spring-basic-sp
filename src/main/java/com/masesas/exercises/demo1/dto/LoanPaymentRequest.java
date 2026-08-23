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
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pembayaran satu angsuran atas pinjaman yang sudah dicairkan")
public class LoanPaymentRequest {

    @NotNull
    @Schema(description = "ID pengajuan pinjaman yang dibayar", example = "1")
    private Integer idLoanApplication;

    @NotNull
    @Positive
    @Schema(description = "Angsuran ke berapa, unik per pinjaman", example = "1")
    private Integer angsuranKe;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Schema(description = "Nominal yang dibayar", example = "1000000.00")
    private BigDecimal jumlahBayar;

    @NotNull
    @Schema(description = "Tanggal pembayaran dalam format ISO yyyy-MM-dd", example = "2026-08-23")
    private LocalDate tanggalBayar;

    @NotBlank
    @Size(max = 20)
    @Schema(description = "Cara pembayaran", example = "TRANSFER")
    private String metode;
}
