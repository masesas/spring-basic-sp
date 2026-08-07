package com.masesas.exercises.demo1.model;

import java.time.LocalDate;

/** Result set 3 dari sp_proses_karyawan: daftar training yang diikuti karyawan. */
public record TrainingRingkas(Integer id, String tema, String pengajar, LocalDate tanggal) {

    /** Urutan kolom sama dengan urutan di cursor p_training. */
    public static TrainingRingkas fromRow(Object[] row) {
        return new TrainingRingkas(
                ((Number) row[0]).intValue(),
                (String) row[1],
                (String) row[2],
                SpRowUtils.toLocalDate(row[3]));
    }
}
