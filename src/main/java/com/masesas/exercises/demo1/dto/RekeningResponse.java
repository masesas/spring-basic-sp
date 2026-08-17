package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.entity.Rekening;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rekening bank karyawan")
public class RekeningResponse {

    @Schema(description = "ID rekening", example = "1")
    private Integer id;
    @Schema(description = "ID karyawan pemilik rekening", example = "12")
    private Integer idKaryawan;
    @Schema(description = "Nama karyawan pemilik rekening", example = "Budi Santoso")
    private String namaKaryawan;
    @Schema(description = "Nama bank atau jenis rekening", example = "BCA")
    private String jenis;
    @Schema(description = "Nama pemilik seperti tertulis di rekening", example = "Budi Santoso")
    private String nama;
    @Schema(description = "Nomor rekening", example = "1234567890")
    private String rekening;
    @Schema(description = "Waktu pembuatan baris", example = "2026-08-17T16:15:02.902570Z")
    private Instant createdDate;
    @Schema(description = "Waktu perubahan terakhir", example = "2026-08-17T16:15:02.902570Z")
    private Instant updatedDate;

    public static RekeningResponse from(Rekening rekening) {
        Karyawan karyawan = rekening.getIdKaryawan();
        return new RekeningResponse(
                rekening.getId(),
                karyawan == null ? null : karyawan.getId(),
                karyawan == null ? null : karyawan.getNama(),
                rekening.getJenis(),
                rekening.getNama(),
                rekening.getRekening(),
                rekening.getCreatedDate(),
                rekening.getUpdatedDate());
    }
}
