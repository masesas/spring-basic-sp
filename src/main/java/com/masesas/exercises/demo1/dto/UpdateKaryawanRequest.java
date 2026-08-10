package com.masesas.exercises.demo1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateKaryawanRequest {

    @NotBlank(message = "nama wajib diisi")
    @Size(max = 100, message = "nama maksimal 100 karakter")
    private String nama;

    @Size(max = 255, message = "alamat maksimal 255 karakter")
    private String alamat;

    private LocalDate dob;

    @Pattern(regexp = "AKTIF|NONAKTIF", message = "status harus AKTIF atau NONAKTIF")
    private String status;
}
