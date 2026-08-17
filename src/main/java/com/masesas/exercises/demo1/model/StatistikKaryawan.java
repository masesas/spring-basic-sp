package com.masesas.exercises.demo1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/** Hasil sp_statistik_karyawan_by_status; nama field dibuat camelCase tanpa suffix _out. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Hasil sp_statistik_karyawan_by_status dari beberapa OUT parameter")
public class StatistikKaryawan {

    @Schema(description = "Jumlah karyawan pada status yang diminta", example = "42")
    private Integer total;
    @Schema(description = "Rata-rata umur dalam tahun", example = "31.5")
    private BigDecimal umurRata;
    @Schema(description = "Umur termuda", example = "22")
    private Integer umurMinimum;
    @Schema(description = "Umur tertua", example = "58")
    private Integer umurMaksimum;

    /** Key Map mengikuti nama OUT parameter di stored procedure. */
    public static StatistikKaryawan fromMap(Map<String, Object> out) {
        return new StatistikKaryawan(
                toInteger(out.get("total_out")),
                toBigDecimal(out.get("umur_rata_out")),
                toInteger(out.get("umur_minimum_out")),
                toInteger(out.get("umur_maksimum_out")));
    }

    private static Integer toInteger(Object nilai) {
        return nilai == null ? null : ((Number) nilai).intValue();
    }

    private static BigDecimal toBigDecimal(Object nilai) {
        if (nilai == null) {
            return null;
        }
        return nilai instanceof BigDecimal angka ? angka : BigDecimal.valueOf(((Number) nilai).doubleValue());
    }
}
