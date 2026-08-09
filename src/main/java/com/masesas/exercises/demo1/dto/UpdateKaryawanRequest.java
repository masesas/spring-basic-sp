package com.masesas.exercises.demo1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateKaryawanRequest(
        @NotBlank(message = "nama wajib diisi")
        @Size(max = 100, message = "nama maksimal 100 karakter")
        String nama,

        @Size(max = 255, message = "alamat maksimal 255 karakter")
        String alamat,

        LocalDate dob,

        @Pattern(regexp = "AKTIF|NONAKTIF", message = "status harus AKTIF atau NONAKTIF")
        String status) {
}
