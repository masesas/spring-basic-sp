package com.masesas.exercises.demo1.dto;

import java.time.LocalDate;

public record UpdateKaryawanRequest(
        String nama,
        String alamat,
        LocalDate dob,
        String status) {
}
