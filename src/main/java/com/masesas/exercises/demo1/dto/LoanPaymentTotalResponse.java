package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Akumulasi pembayaran atas satu pinjaman")
public class LoanPaymentTotalResponse {

    @Schema(description = "Jumlah seluruh angsuran yang sudah dibayar", example = "3000000.00")
    private BigDecimal totalDibayar;
}
