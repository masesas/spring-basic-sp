package com.masesas.exercises.demo1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Token dalam bentuk baku OAuth2, supaya bisa dipasang otomatis oleh klien")
public class TokenResponse {

    @JsonProperty("access_token")
    @Schema(description = "JWT yang dipakai sebagai bearer token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private final String accessToken;

    @JsonProperty("token_type")
    @Schema(description = "Selalu Bearer", example = "Bearer")
    private final String tokenType;

    @JsonProperty("expires_in")
    @Schema(description = "Masa berlaku token dalam detik", example = "900")
    private final long expiresIn;
}
