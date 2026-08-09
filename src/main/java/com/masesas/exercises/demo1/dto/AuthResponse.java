package com.masesas.exercises.demo1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuthResponse {

    private final String token;
    private final String tipe;
    private final List<String> roles;
}
