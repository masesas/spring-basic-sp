package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.entity.KaryawanTraining;
import com.masesas.exercises.demo1.entity.Training;

import java.time.Instant;
import java.time.LocalDate;

public record KaryawanTrainingResponse(
        Integer id,
        Integer idKaryawan,
        String namaKaryawan,
        Integer idTraining,
        String tema,
        String pengajar,
        LocalDate tanggal,
        Instant createdDate,
        Instant updatedDate) {

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
