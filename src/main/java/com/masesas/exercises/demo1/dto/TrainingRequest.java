package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Materi pelatihan")
public class TrainingRequest {

    @Schema(description = "Tema pelatihan", example = "Keamanan Aplikasi Web")
    private String tema;
    @Schema(description = "Nama pengajar", example = "Dewi Lestari")
    private String pengajar;
}
