package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Data karyawan dari jalur JdbcTemplate")
public class Karyawan2Response {
    @Schema(description = "ID karyawan", example = "1")
    private Integer id;
    @Schema(description = "Nama lengkap karyawan", example = "Budi Santoso")
    private String nama;
    @Schema(description = "Alamat tempat tinggal", example = "Jl. Melati No. 5, Bandung")
    private String alamat;
    @Schema(description = "Tanggal lahir", example = "1995-04-17")
    private LocalDate dob;
    @Schema(description = "Status kepegawaian", example = "AKTIF")
    private  String status;

}
