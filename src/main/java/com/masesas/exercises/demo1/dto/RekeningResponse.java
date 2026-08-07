package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.entity.Rekening;

import java.time.Instant;

public record RekeningResponse(
        Integer id,
        Integer idKaryawan,
        String namaKaryawan,
        String jenis,
        String nama,
        String rekening,
        Instant createdDate,
        Instant updatedDate) {

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
