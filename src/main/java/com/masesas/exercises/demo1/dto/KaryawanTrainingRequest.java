package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pendaftaran karyawan ke satu pelatihan")
public class KaryawanTrainingRequest {

    @Schema(description = "ID karyawan peserta", example = "12")
    private Integer idKaryawan;
    @Schema(description = "ID pelatihan yang diikuti", example = "3")
    private Integer idTraining;
    @Schema(description = "Tanggal keikutsertaan dalam format ISO yyyy-MM-dd", example = "2026-08-17")
    private LocalDate tanggal;
}
