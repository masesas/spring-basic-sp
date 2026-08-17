package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "Hasil login: token berikut identitas peran pemiliknya")
public class AuthResponse {

    @Schema(description = "JWT yang dipakai sebagai bearer token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private final String token;
    @Schema(description = "Tipe akun pemilik token", example = "KARYAWAN")
    private final String tipe;
    @Schema(description = "Peran yang melekat pada akun")
    private final List<String> roles;
}
