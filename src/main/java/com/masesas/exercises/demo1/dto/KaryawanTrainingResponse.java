package com.masesas.exercises.demo1.dto;

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
public class KaryawanTrainingResponse {

    private Integer id;
    private Integer idKaryawan;
    private String namaKaryawan;
    private Integer idTraining;
    private String tema;
    private String pengajar;
    private LocalDate tanggal;
    private Instant createdDate;
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
