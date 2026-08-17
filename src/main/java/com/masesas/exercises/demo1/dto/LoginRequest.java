package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kredensial login")
public class LoginRequest {

    @NotBlank(message = "username wajib diisi")
    @Schema(description = "Email akun", example = "hr@masesas.test")
    private String username;

    @NotBlank(message = "password wajib diisi")
    @Schema(description = "Password akun", example = "rahasia123")
    private String password;
}
