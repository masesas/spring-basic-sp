package com.masesas.exercises.demo1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KaryawanTrainingRequest {

    private Integer idKaryawan;
    private Integer idTraining;
    private LocalDate tanggal;
}
