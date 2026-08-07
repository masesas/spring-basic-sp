package com.masesas.exercises.demo1.dto;

import java.time.LocalDate;

public record Karyawan2Request(
        String nama,
        String alamat,
        LocalDate dob,
        String status) {
}
