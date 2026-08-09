package com.masesas.exercises.demo1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerRegisterRequest {

    @NotBlank(message = "nama wajib diisi")
    @Size(max = 100, message = "nama maksimal 100 karakter")
    private String nama;

    @NotBlank(message = "email wajib diisi")
    @Email(message = "format email tidak valid")
    @Size(max = 150, message = "email maksimal 150 karakter")
    private String email;

    @NotBlank(message = "password wajib diisi")
    @Size(min = 8, message = "password minimal 8 karakter")
    private String password;
}
