package com.masesas.exercises.demo1.dto;

public record RekeningRequest(
        Integer idKaryawan,
        String jenis,
        String nama,
        String rekening) {
}
