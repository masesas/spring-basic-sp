package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.Karyawan;

import java.time.Instant;
import java.time.LocalDate;

public record KaryawanResponse(
        Integer id,
        String nama,
        String alamat,
        LocalDate dob,
        String status,
        DetailKaryawanResponse detail,
        Instant createdDate,
        Instant updatedDate) {

    public static KaryawanResponse from(Karyawan karyawan) {
        return new KaryawanResponse(
                karyawan.getId(),
                karyawan.getNama(),
                karyawan.getAlamat(),
                karyawan.getDob(),
                karyawan.getStatus(),
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
