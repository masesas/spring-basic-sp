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
@Schema(description = "Jenis dokumen yang menyertai pengajuan pinjaman")
public class LoanDocumentTypeRequest {

    @NotBlank
    @Size(max = 30)
    @Schema(description = "Kode jenis dokumen, unik", example = "KK")
    private String kode;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Nama jenis dokumen", example = "Kartu Keluarga")
    private String nama;

    @Schema(description = "Wajib dilampirkan, default false", example = "true")
    private Boolean wajib;
}
