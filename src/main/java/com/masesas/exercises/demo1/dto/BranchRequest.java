package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data cabang")
public class BranchRequest {

    @NotBlank
    @Size(max = 20)
    @Schema(description = "Kode cabang, unik", example = "BR03")
    private String kode;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Nama cabang", example = "Cabang Surabaya")
    private String nama;

    @Size(max = 500)
    @Schema(description = "Alamat cabang", example = "Jl. Pemuda No. 33, Surabaya")
    private String alamat;
}
