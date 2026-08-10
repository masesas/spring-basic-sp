package com.masesas.exercises.demo1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KaryawanLengkap {

    private Integer id;
    private String nama;
    private String alamat;
    private String status;
    private LocalDate dob;
    private Integer umur;
    private String kategoriUmur;
    private String nik;
    private String npwp;
    private Integer jumlahRekening;

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
