package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Catatan yang menyertai keputusan atas pengajuan")
public class LoanApplicationDecisionRequest {

    @Size(max = 255)
    @Schema(description = "Alasan atau catatan keputusan. Wajib diisi saat menolak.",
            example = "Penghasilan tidak memenuhi syarat")
    private String catatan;
}
