package com.masesas.exercises.demo1.dto;

import java.time.LocalDate;

public record KaryawanTrainingRequest(
        Integer idKaryawan,
        Integer idTraining,
        LocalDate tanggal) {
}
