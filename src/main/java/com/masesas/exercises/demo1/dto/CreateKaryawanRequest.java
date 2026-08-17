package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
@Schema(description = "Data karyawan baru")
public class CreateKaryawanRequest {

    @NotBlank(message = "nama wajib diisi")
    @Size(max = 100, message = "nama maksimal 100 karakter")
    @Schema(description = "Nama lengkap karyawan", example = "Budi Santoso")
    private String nama;

    @Size(max = 255, message = "alamat maksimal 255 karakter")
    @Schema(description = "Alamat tempat tinggal", example = "Jl. Melati No. 5, Bandung")
    private String alamat;

    @Schema(description = "Tanggal lahir dalam format ISO yyyy-MM-dd", example = "1995-04-17")
    private LocalDate dob;

    @Pattern(regexp = "AKTIF|NONAKTIF", message = "status harus AKTIF atau NONAKTIF")
    @Schema(description = "Status kepegawaian, hanya AKTIF atau NONAKTIF", example = "AKTIF")
    private String status;

    @Valid
    @Schema(description = "Detail identitas, boleh dikosongkan")
    private DetailKaryawanRequest detail;
}
