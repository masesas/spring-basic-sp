package com.masesas.exercises.demo1.dto;

import jakarta.validation.constraints.Pattern;

public record DetailKaryawanRequest(
        @Pattern(regexp = "\\d{16}", message = "nik harus 16 digit angka")
        String nik,

        @Pattern(regexp = "\\d{15}", message = "npwp harus 15 digit angka")
        String npwp) {
}
