package com.masesas.exercises.demo1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HasilProsesKaryawan {

    private KaryawanLengkap karyawan;
    private List<RekeningRingkas> daftarRekening;
    private List<TrainingRingkas> daftarTraining;
}
