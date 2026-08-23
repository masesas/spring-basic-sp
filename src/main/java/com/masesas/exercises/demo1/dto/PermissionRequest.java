package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Izin berbutir halus yang bisa dipasang ke peran")
public class PermissionRequest {

    @NotBlank
    @Size(max = 60)
    @Schema(description = "Kode izin, unik, dipakai apa adanya di hasAuthority(...)",
            example = "LOAN_APPLICATION_APPROVE")
    private String kode;

    @Size(max = 150)
    @Schema(description = "Penjelasan singkat", example = "Menyetujui pengajuan pinjaman")
    private String deskripsi;
}
