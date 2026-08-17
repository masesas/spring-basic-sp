package com.masesas.exercises.demo1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data karyawan hasil stored procedure, sudah termasuk nilai turunan")
public class KaryawanLengkap {

    @Schema(description = "ID karyawan", example = "7")
    private Integer id;
    @Schema(description = "Nama lengkap karyawan", example = "Budi Santoso")
    private String nama;
    @Schema(description = "Alamat tempat tinggal", example = "Jl. Melati No. 5, Bandung")
    private String alamat;
    @Schema(description = "Status kepegawaian", example = "AKTIF")
    private String status;
    @Schema(description = "Tanggal lahir", example = "1995-04-17")
    private LocalDate dob;
    @Schema(description = "Umur dalam tahun, dihitung di stored procedure", example = "31")
    private Integer umur;
    @Schema(description = "Pengelompokan umur dari stored procedure", example = "DEWASA")
    private String kategoriUmur;
    @Schema(description = "NIK karyawan", example = "3204012345678901")
    private String nik;
    @Schema(description = "NPWP karyawan", example = "123456789012345")
    private String npwp;
    @Schema(description = "Banyaknya rekening milik karyawan", example = "2")
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
