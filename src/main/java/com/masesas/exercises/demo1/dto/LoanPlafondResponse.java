package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.LoanPlafond;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Plafond kredit customer beserta pemakaiannya")
public class LoanPlafondResponse {

    @Schema(description = "ID plafond", example = "1")
    private Integer id;
    @Schema(description = "ID customer", example = "1")
    private Integer idCustomer;
    @Schema(description = "Nama customer", example = "Customer Satu")
    private String namaCustomer;
    @Schema(description = "Batas kredit yang diberikan", example = "50000000.00")
    private BigDecimal plafondTotal;
    @Schema(description = "Bagian plafond yang sedang dipakai pinjaman berjalan", example = "10000000.00")
    private BigDecimal plafondTerpakai;
    @Schema(description = "Selisih total dan terpakai, dihitung bukan disimpan", example = "40000000.00")
    private BigDecimal sisa;
    @Schema(description = "Nomor versi untuk penguncian optimistis", example = "0")
    private Long version;
    @Schema(description = "Waktu perubahan terakhir", example = "2026-08-23T15:04:05Z")
    private Instant updatedDate;

    public static LoanPlafondResponse from(LoanPlafond plafond) {
        return new LoanPlafondResponse(
                plafond.getId(),
                plafond.getCustomer().getId(),
                plafond.getCustomer().getNama(),
                plafond.getPlafondTotal(),
                plafond.getPlafondTerpakai(),
                plafond.sisa(),
                plafond.getVersion(),
                plafond.getUpdatedDate());
    }
}
