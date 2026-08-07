package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.DetailKaryawan;

import java.time.Instant;

public record DetailKaryawanResponse(
        Integer id,
        String nik,
        String npwp,
        Instant createdDate,
        Instant updatedDate) {

    public static DetailKaryawanResponse from(DetailKaryawan detail) {
        return new DetailKaryawanResponse(
                detail.getId(),
                detail.getNik(),
                detail.getNpwp(),
                detail.getCreatedDate(),
                detail.getUpdatedDate());
    }
}
