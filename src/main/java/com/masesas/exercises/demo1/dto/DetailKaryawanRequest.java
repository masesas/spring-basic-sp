package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data identitas karyawan yang disimpan terenkripsi")
public class DetailKaryawanRequest {

    @Pattern(regexp = "\\d{16}", message = "nik harus 16 digit angka")
    @Schema(description = "NIK, tepat 16 digit angka. Disimpan terenkripsi di database", example = "3204012345678901")
    private String nik;

    @Pattern(regexp = "\\d{15}", message = "npwp harus 15 digit angka")
    @Schema(description = "NPWP, tepat 15 digit angka. Disimpan terenkripsi di database", example = "123456789012345")
    private String npwp;
}
