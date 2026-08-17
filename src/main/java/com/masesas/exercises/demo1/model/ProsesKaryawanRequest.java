package com.masesas.exercises.demo1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Perubahan yang dikirim ke sp_proses_karyawan")
public class ProsesKaryawanRequest {

    @Schema(description = "Nama baru, kosongkan bila tidak diubah", example = "Budi Santoso")
    private String nama;
    @Schema(description = "Alamat baru, kosongkan bila tidak diubah", example = "Jl. Melati No. 5, Bandung")
    private String alamat;
    @Schema(description = "Status kepegawaian baru", example = "AKTIF")
    private String status;
    @Schema(description = "Mode pemrosesan yang diteruskan ke stored procedure", example = "LENGKAP")
    private String mode;
}
