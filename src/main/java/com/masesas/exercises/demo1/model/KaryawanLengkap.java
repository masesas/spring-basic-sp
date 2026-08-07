package com.masesas.exercises.demo1.model;

import java.time.LocalDate;

/** Result set 1 dari sp_proses_karyawan: karyawan + detail karyawan. */
public record KaryawanLengkap(
        Integer id,
        String nama,
        String alamat,
        String status,
        LocalDate dob,
        Integer umur,
        String kategoriUmur,
        String nik,
        String npwp,
        Integer jumlahRekening) {

    /** Urutan kolom sama dengan urutan di cursor p_karyawan. */
    public static KaryawanLengkap fromRow(Object[] row) {
        return new KaryawanLengkap(
                ((Number) row[0]).intValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                SpRowUtils.toLocalDate(row[4]),
                ((Number) row[5]).intValue(),
                (String) row[6],
                (String) row[7],
                (String) row[8],
                ((Number) row[9]).intValue());
    }
}
