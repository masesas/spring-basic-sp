package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.entity.KaryawanTraining;
import com.masesas.exercises.demo1.entity.Training;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keikutsertaan karyawan pada satu pelatihan")
public class KaryawanTrainingResponse {

    @Schema(description = "ID keikutsertaan", example = "1")
    private Integer id;
    @Schema(description = "ID karyawan peserta", example = "12")
    private Integer idKaryawan;
    @Schema(description = "Nama karyawan peserta", example = "Budi Santoso")
    private String namaKaryawan;
    @Schema(description = "ID pelatihan", example = "3")
    private Integer idTraining;
    @Schema(description = "Tema pelatihan", example = "Keamanan Aplikasi Web")
    private String tema;
    @Schema(description = "Nama pengajar", example = "Dewi Lestari")
    private String pengajar;
    @Schema(description = "Tanggal keikutsertaan", example = "2026-08-17")
    private LocalDate tanggal;
    @Schema(description = "Waktu pembuatan baris", example = "2026-08-17T16:15:02.902570Z")
    private Instant createdDate;
    @Schema(description = "Waktu perubahan terakhir", example = "2026-08-17T16:15:02.902570Z")
    private Instant updatedDate;

    public static KaryawanTrainingResponse from(KaryawanTraining karyawanTraining) {
        Karyawan karyawan = karyawanTraining.getIdKaryawan();
        Training training = karyawanTraining.getIdTraining();
        return new KaryawanTrainingResponse(
                karyawanTraining.getId(),
                karyawan == null ? null : karyawan.getId(),
                karyawan == null ? null : karyawan.getNama(),
                training == null ? null : training.getId(),
                training == null ? null : training.getTema(),
                training == null ? null : training.getPengajar(),
                karyawanTraining.getTanggal(),
                karyawanTraining.getCreatedDate(),
                karyawanTraining.getUpdatedDate());
    }
}
