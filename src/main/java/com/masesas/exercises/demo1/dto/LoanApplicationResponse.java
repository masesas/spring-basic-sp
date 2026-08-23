package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.Branch;
import com.masesas.exercises.demo1.entity.LoanApplication;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pengajuan pinjaman beserta status dan jejak auditnya")
public class LoanApplicationResponse {

    @Schema(description = "ID pengajuan", example = "1")
    private Integer id;
    @Schema(description = "ID customer pengaju", example = "1")
    private Integer idCustomer;
    @Schema(description = "Nama customer pengaju", example = "Customer Satu")
    private String namaCustomer;
    @Schema(description = "ID produk pinjaman", example = "1")
    private Integer idLoanProduct;
    @Schema(description = "Kode produk pinjaman", example = "KTA")
    private String kodeLoanProduct;
    @Schema(description = "ID cabang pemroses", example = "1")
    private Integer idBranch;
    @Schema(description = "Kode cabang pemroses", example = "BR01")
    private String kodeBranch;
    @Schema(description = "Nominal yang diajukan", example = "10000000.00")
    private BigDecimal jumlahPengajuan;
    @Schema(description = "Tenor dalam bulan", example = "12")
    private Integer tenorBulan;
    @Schema(description = "Status pengajuan",
            example = "SUBMITTED",
            allowableValues = {"DRAFT", "SUBMITTED", "APPROVED", "REJECTED", "DISBURSED", "CANCELLED"})
    private String status;
    @Schema(description = "Catatan keputusan terakhir", example = "Penghasilan tidak memenuhi syarat")
    private String catatan;
    @Schema(description = "Nomor versi untuk penguncian optimistis", example = "0")
    private Long version;
    @Schema(description = "Waktu pembuatan baris", example = "2026-08-23T15:04:05Z")
    private Instant createdDate;
    @Schema(description = "Waktu perubahan terakhir", example = "2026-08-23T15:04:05Z")
    private Instant updatedDate;

    public static LoanApplicationResponse from(LoanApplication pengajuan) {
        Branch branch = pengajuan.getBranch();
        return new LoanApplicationResponse(
                pengajuan.getId(),
                pengajuan.getCustomer().getId(),
                pengajuan.getCustomer().getNama(),
                pengajuan.getLoanProduct().getId(),
                pengajuan.getLoanProduct().getKode(),
                branch == null ? null : branch.getId(),
                branch == null ? null : branch.getKode(),
                pengajuan.getJumlahPengajuan(),
                pengajuan.getTenorBulan(),
                pengajuan.getStatus().name(),
                pengajuan.getCatatan(),
                pengajuan.getVersion(),
                pengajuan.getCreatedDate(),
                pengajuan.getUpdatedDate());
    }
}
