package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bentuk ringkas hasil login")
public class LoginResponse {

    @Schema(description = "JWT yang dipakai sebagai bearer token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;
    @Schema(description = "Peran utama akun", example = "HR")
    private String role;
}
