package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pengajuan pinjaman. Pemiliknya diambil dari token, bukan dari badan permintaan.")
public class LoanApplicationRequest {

    @NotNull
    @Schema(description = "ID produk pinjaman", example = "1")
    private Integer idLoanProduct;

    @Schema(description = "ID cabang pemroses, boleh kosong", example = "1")
    private Integer idBranch;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Schema(description = "Nominal yang diajukan", example = "10000000.00")
    private BigDecimal jumlahPengajuan;

    @NotNull
    @Positive
    @Schema(description = "Tenor dalam bulan", example = "12")
    private Integer tenorBulan;
}
