package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.LoanPayment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pembayaran angsuran beserta jejak auditnya")
public class LoanPaymentResponse {

    @Schema(description = "ID pembayaran", example = "1")
    private Integer id;
    @Schema(description = "ID pengajuan pinjaman yang dibayar", example = "1")
    private Integer idLoanApplication;
    @Schema(description = "Angsuran ke berapa", example = "1")
    private Integer angsuranKe;
    @Schema(description = "Nominal yang dibayar", example = "1000000.00")
    private BigDecimal jumlahBayar;
    @Schema(description = "Tanggal pembayaran", example = "2026-08-23")
    private LocalDate tanggalBayar;
    @Schema(description = "Cara pembayaran", example = "TRANSFER")
    private String metode;
    @Schema(description = "Waktu pembuatan baris", example = "2026-08-23T15:04:05Z")
    private Instant createdDate;

    public static LoanPaymentResponse from(LoanPayment payment) {
        return new LoanPaymentResponse(
                payment.getId(),
                payment.getLoanApplication().getId(),
                payment.getAngsuranKe(),
                payment.getJumlahBayar(),
                payment.getTanggalBayar(),
                payment.getMetode(),
                payment.getCreatedDate());
    }
}
