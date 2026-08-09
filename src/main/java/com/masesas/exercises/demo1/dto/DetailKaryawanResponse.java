package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.DetailKaryawan;

import java.time.Instant;

public record DetailKaryawanResponse(
        Integer id,
        String nik,
        String npwp,
        Instant createdDate,
        Instant updatedDate) {

    /** Bentuk baku: nomor identitas selalu tersamar. Aman secara bawaan. */
    public static DetailKaryawanResponse from(DetailKaryawan detail) {
        return new DetailKaryawanResponse(
                detail.getId(),
                samarkan(detail.getNik()),
                samarkan(detail.getNpwp()),
                detail.getCreatedDate(),
                detail.getUpdatedDate());
    }

    /** Nomor identitas utuh. Hanya dipakai controller demo yang sengaja dibuat rentan. */
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
