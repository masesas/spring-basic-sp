package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.Karyawan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KaryawanResponse {

    private Integer id;
    private String nama;
    private String alamat;
    private LocalDate dob;
    private String status;
    private String avatar;
    private DetailKaryawanResponse detail;
    private Instant createdDate;
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
