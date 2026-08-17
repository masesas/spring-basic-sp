package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "Ringkasan endpoint yang bisa diakses satu peran")
public class RoleAksesResponse {

    @Schema(description = "Nama peran", example = "ADMIN")
    private final String peran;
    @Schema(description = "Banyaknya endpoint yang bisa diakses peran ini", example = "32")
    private final int jumlah;
    @Schema(description = "Daftar endpoint yang bisa diakses")
    private final List<EndpointAksesResponse> endpoint;
}
