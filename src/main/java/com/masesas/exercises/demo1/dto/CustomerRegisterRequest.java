package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Data pendaftaran customer baru")
public class CustomerRegisterRequest {

    @NotBlank(message = "nama wajib diisi")
    @Size(max = 100, message = "nama maksimal 100 karakter")
    @Schema(description = "Nama lengkap customer", example = "Siti Rahma")
    private String nama;

    @NotBlank(message = "email wajib diisi")
    @Email(message = "format email tidak valid")
    @Size(max = 150, message = "email maksimal 150 karakter")
    @Schema(description = "Email, dipakai sebagai username saat login", example = "siti@contoh.test")
    private String email;

    @NotBlank(message = "password wajib diisi")
    @Size(min = 8, message = "password minimal 8 karakter")
    @Schema(description = "Password, minimal 8 karakter", example = "rahasia123")
    private String password;
}
