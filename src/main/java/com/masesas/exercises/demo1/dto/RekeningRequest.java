package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rekening bank milik karyawan")
public class RekeningRequest {

    @Schema(description = "ID karyawan pemilik rekening", example = "12")
    private Integer idKaryawan;
    @Schema(description = "Nama bank atau jenis rekening", example = "BCA")
    private String jenis;
    @Schema(description = "Nama pemilik seperti tertulis di rekening", example = "Budi Santoso")
    private String nama;
    @Schema(description = "Nomor rekening", example = "1234567890")
    private String rekening;
}
