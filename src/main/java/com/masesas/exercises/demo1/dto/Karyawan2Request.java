package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data karyawan untuk jalur JdbcTemplate")
public class Karyawan2Request {

    @Schema(description = "Nama lengkap karyawan", example = "Budi Santoso")
    private String nama;
    @Schema(description = "Alamat tempat tinggal", example = "Jl. Melati No. 5, Bandung")
    private String alamat;
    @Schema(description = "Tanggal lahir dalam format ISO yyyy-MM-dd", example = "1995-04-17")
    private LocalDate dob;
    @Schema(description = "Status kepegawaian", example = "AKTIF")
    private String status;
}
