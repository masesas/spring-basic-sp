package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Alamat berkas foto")
public class FotoRequest {

    @Schema(description = "Alamat berkas foto", example = "https://contoh.test/foto.png")
    private String url;
}
