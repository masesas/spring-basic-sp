package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.Permission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Izin beserta jejak auditnya")
public class PermissionResponse {

    @Schema(description = "ID izin", example = "13")
    private Integer id;
    @Schema(description = "Kode izin", example = "LOAN_APPLICATION_APPROVE")
    private String kode;
    @Schema(description = "Penjelasan singkat", example = "Menyetujui pengajuan pinjaman")
    private String deskripsi;
    @Schema(description = "Waktu pembuatan baris", example = "2026-08-23T15:04:05Z")
    private Instant createdDate;
    @Schema(description = "Waktu perubahan terakhir", example = "2026-08-23T15:04:05Z")
    private Instant updatedDate;

    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getKode(),
                permission.getDeskripsi(),
                permission.getCreatedDate(),
                permission.getUpdatedDate());
    }
}
