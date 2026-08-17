package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.masesas.exercises.demo1.entity.Training;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Materi pelatihan beserta jejak auditnya")
public class TrainingResponse {

    @Schema(description = "ID pelatihan", example = "1")
    private Integer id;
    @Schema(description = "Tema pelatihan", example = "Keamanan Aplikasi Web")
    private String tema;
    @Schema(description = "Nama pengajar", example = "Dewi Lestari")
    private String pengajar;
    @Schema(description = "Waktu pembuatan baris", example = "2026-08-17T16:15:02.902570Z")
    private Instant createdDate;
    @Schema(description = "Waktu perubahan terakhir", example = "2026-08-17T16:15:02.902570Z")
    private Instant updatedDate;

    public static TrainingResponse from(Training training) {
        return new TrainingResponse(
                training.getId(),
                training.getTema(),
                training.getPengajar(),
                training.getAudit().getCreatedDate(),
                training.getAudit().getUpdatedDate());
    }
}
