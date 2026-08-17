package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.masesas.exercises.demo1.entity.Karyawan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data karyawan beserta detail dan jejak auditnya")
public class KaryawanResponse {

    @Schema(description = "ID karyawan", example = "1")
    private Integer id;
    @Schema(description = "Nama lengkap karyawan", example = "Budi Santoso")
    private String nama;
    @Schema(description = "Alamat tempat tinggal", example = "Jl. Melati No. 5, Bandung")
    private String alamat;
    @Schema(description = "Tanggal lahir", example = "1995-04-17")
    private LocalDate dob;
    @Schema(description = "Status kepegawaian", example = "AKTIF")
    private String status;
    @Schema(description = "Nama berkas foto yang tersimpan di server", example = "1-avatar.png")
    private String avatar;
    @Schema(description = "Detail identitas, null bila belum diisi atau sudah dihapus")
    private DetailKaryawanResponse detail;
    @Schema(description = "Waktu pembuatan baris", example = "2026-08-17T16:15:02.902570Z")
    private Instant createdDate;
    @Schema(description = "Waktu perubahan terakhir", example = "2026-08-17T16:15:02.902570Z")
    private Instant updatedDate;

    public static KaryawanResponse from(Karyawan karyawan) {
        return new KaryawanResponse(
                karyawan.getId(),
                karyawan.getNama(),
                karyawan.getAlamat(),
                karyawan.getDob(),
                karyawan.getStatus(),
                karyawan.getAvatar(),
                detailDari(karyawan),
                karyawan.getCreatedDate(),
                karyawan.getUpdatedDate());
    }

    private static DetailKaryawanResponse detailDari(Karyawan karyawan) {
        var detail = karyawan.getDetailKaryawan();
        if (detail == null || detail.getDeletedDate() != null) {
            return null;
        }
        return DetailKaryawanResponse.from(detail);
    }
}
