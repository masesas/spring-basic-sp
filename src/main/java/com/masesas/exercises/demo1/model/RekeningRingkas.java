package com.masesas.exercises.demo1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rekening karyawan dalam bentuk ringkas")
public class RekeningRingkas {

    @Schema(description = "ID rekening", example = "1")
    private Integer id;
    @Schema(description = "Nama pemilik seperti tertulis di rekening", example = "Budi Santoso")
    private String nama;
    @Schema(description = "Nama bank atau jenis rekening", example = "BCA")
    private String jenis;
    @Schema(description = "Nomor rekening", example = "1234567890")
    private String rekening;

    public static RekeningRingkas fromRow(Object[] row) {
        return new RekeningRingkas(
                ((Number) row[0]).intValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3]);
    }
}
