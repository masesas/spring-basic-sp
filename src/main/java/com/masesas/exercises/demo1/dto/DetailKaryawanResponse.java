package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.masesas.exercises.demo1.entity.DetailKaryawan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detail karyawan, dengan nik dan npwp tersamar")
public class DetailKaryawanResponse {

    @Schema(description = "ID detail karyawan", example = "1")
    private Integer id;
    @Schema(description = "NIK, hanya empat digit terakhir yang ditampilkan", example = "************8901")
    private String nik;
    @Schema(description = "NPWP, hanya empat digit terakhir yang ditampilkan", example = "***********2345")
    private String npwp;
    @Schema(description = "Waktu pembuatan baris", example = "2026-08-17T16:15:02.902570Z")
    private Instant createdDate;
    @Schema(description = "Waktu perubahan terakhir", example = "2026-08-17T16:15:02.902570Z")
    private Instant updatedDate;

    public static DetailKaryawanResponse from(DetailKaryawan detail) {
        return new DetailKaryawanResponse(
                detail.getId(),
                samarkan(detail.getNik()),
                samarkan(detail.getNpwp()),
                detail.getCreatedDate(),
                detail.getUpdatedDate());
    }

    public static DetailKaryawanResponse fromLengkap(DetailKaryawan detail) {
        return new DetailKaryawanResponse(
                detail.getId(),
                detail.getNik(),
                detail.getNpwp(),
                detail.getCreatedDate(),
                detail.getUpdatedDate());
    }

    private static String samarkan(String nomor) {
        if (nomor == null || nomor.length() <= 4) {
            return nomor;
        }
        return "*".repeat(nomor.length() - 4) + nomor.substring(nomor.length() - 4);
    }
}
