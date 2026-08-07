package com.masesas.exercises.demo1.model;

/** Result set 2 dari sp_proses_karyawan: daftar rekening milik karyawan. */
public record RekeningRingkas(Integer id, String nama, String jenis, String rekening) {

    /** Urutan kolom sama dengan urutan di cursor p_rekening. */
    public static RekeningRingkas fromRow(Object[] row) {
        return new RekeningRingkas(
                ((Number) row[0]).intValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3]);
    }
}
