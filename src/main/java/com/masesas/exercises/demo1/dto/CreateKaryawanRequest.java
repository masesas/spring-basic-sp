package com.masesas.exercises.demo1.dto;

import java.time.LocalDate;

public record CreateKaryawanRequest(
        String nama,
        String alamat,
        LocalDate dob,
        String status,
        DetailKaryawanRequest detail) {
}
