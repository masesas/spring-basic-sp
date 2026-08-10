package com.masesas.exercises.demo1.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailKaryawanRequest {

    @Pattern(regexp = "\\d{16}", message = "nik harus 16 digit angka")
    private String nik;

    @Pattern(regexp = "\\d{15}", message = "npwp harus 15 digit angka")
    private String npwp;
}
