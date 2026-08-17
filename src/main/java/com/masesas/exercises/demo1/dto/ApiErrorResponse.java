package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(name = "ApiError", description = "Bentuk badan response untuk semua kegagalan")
public class ApiErrorResponse {

    @Schema(description = "Waktu kejadian dalam UTC", example = "2026-08-17T16:15:02.902570Z")
    private final String timestamp;

    @Schema(description = "Kode status HTTP", example = "404")
    private final int status;

    @Schema(description = "Nama status HTTP", example = "Not Found")
    private final String error;

    @Schema(description = "Penjelasan singkat, tanpa detail internal", example = "Karyawan tidak ditemukan")
    private final String message;
}
