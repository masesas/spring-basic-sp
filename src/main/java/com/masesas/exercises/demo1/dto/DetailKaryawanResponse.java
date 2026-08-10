package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.DetailKaryawan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailKaryawanResponse {

    private Integer id;
    private String nik;
    private String npwp;
    private Instant createdDate;
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
