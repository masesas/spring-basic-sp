package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data karyawan versi teks saja")
public class KaryawanTeksRequest {

    @Schema(description = "Nama lengkap karyawan", example = "Budi Santoso")
    private String nama;
    @Schema(description = "Alamat tempat tinggal", example = "Jl. Melati No. 5, Bandung")
    private String alamat;
}
