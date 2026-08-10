package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.entity.Rekening;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RekeningResponse {

    private Integer id;
    private Integer idKaryawan;
    private String namaKaryawan;
    private String jenis;
    private String nama;
    private String rekening;
    private Instant createdDate;
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
