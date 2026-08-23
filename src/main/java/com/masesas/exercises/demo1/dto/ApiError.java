package com.masesas.exercises.demo1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ApiError", description = "Rincian kegagalan, hanya muncul ketika permintaan gagal")
public class ApiError {

    @Schema(description = "Kode kategori kegagalan", example = "VALIDATION_ERROR")
    private String code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Rincian per field, hanya diisi pada kegagalan validasi",
            example = "[\"nama: tidak boleh kosong\"]")
    private List<String> details;
}
