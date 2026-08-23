package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.RolePermission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Satu baris pemetaan peran ke izin")
public class RolePermissionResponse {

    @Schema(description = "ID pemetaan", example = "41")
    private Integer id;
    @Schema(description = "ID peran", example = "2")
    private Integer idRole;
    @Schema(description = "Nama peran", example = "MANAGER")
    private String namaRole;
    @Schema(description = "ID izin", example = "13")
    private Integer idPermission;
    @Schema(description = "Kode izin", example = "LOAN_APPLICATION_APPROVE")
    private String kodePermission;
    @Schema(description = "Waktu pembuatan baris", example = "2026-08-23T15:04:05Z")
    private Instant createdDate;

    public static RolePermissionResponse from(RolePermission rolePermission) {
        return new RolePermissionResponse(
                rolePermission.getId(),
                rolePermission.getRole().getId(),
                rolePermission.getRole().getNama(),
                rolePermission.getPermission().getId(),
                rolePermission.getPermission().getKode(),
                rolePermission.getCreatedDate());
    }
}
