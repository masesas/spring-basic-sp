package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.Branch;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cabang beserta jejak auditnya")
public class BranchResponse {

    @Schema(description = "ID cabang", example = "1")
    private Integer id;
    @Schema(description = "Kode cabang", example = "BR01")
    private String kode;
    @Schema(description = "Nama cabang", example = "Cabang Jakarta Pusat")
    private String nama;
    @Schema(description = "Alamat cabang", example = "Jl. Merdeka Selatan No. 1, Jakarta Pusat")
    private String alamat;
    @Schema(description = "Waktu pembuatan baris", example = "2026-08-23T15:04:05Z")
    private Instant createdDate;
    @Schema(description = "Waktu perubahan terakhir", example = "2026-08-23T15:04:05Z")
    private Instant updatedDate;

    public static BranchResponse from(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getKode(),
                branch.getNama(),
                branch.getAlamat(),
                branch.getCreatedDate(),
                branch.getUpdatedDate());
    }
}
