package com.masesas.exercises.demo1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pelatihan yang diikuti karyawan dalam bentuk ringkas")
public class TrainingRingkas {

    @Schema(description = "ID pelatihan", example = "3")
    private Integer id;
    @Schema(description = "Tema pelatihan", example = "Keamanan Aplikasi Web")
    private String tema;
    @Schema(description = "Nama pengajar", example = "Dewi Lestari")
    private String pengajar;
    @Schema(description = "Tanggal keikutsertaan", example = "2026-08-17")
    private LocalDate tanggal;

    public static TrainingRingkas fromRow(Object[] row) {
        return new TrainingRingkas(
                ((Number) row[0]).intValue(),
                (String) row[1],
                (String) row[2],
                SpRowUtils.toLocalDate(row[3]));
    }
}
