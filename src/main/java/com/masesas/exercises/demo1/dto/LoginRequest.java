package com.masesas.exercises.demo1.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "username wajib diisi")
    private String username;

    @NotBlank(message = "password wajib diisi")
    private String password;
}
