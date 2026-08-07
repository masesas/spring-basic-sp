package com.masesas.exercises.demo1.model;

import java.util.List;

/** Pembungkus 3 result set yang dikembalikan sp_proses_karyawan. */
public record HasilProsesKaryawan(
        KaryawanLengkap karyawan,
        List<RekeningRingkas> daftarRekening,
        List<TrainingRingkas> daftarTraining) {
}
