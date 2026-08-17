package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Slip gaji baru")
public class PayrollRequest {

    @NotNull(message = "idKaryawan wajib diisi")
    @Schema(description = "ID karyawan penerima gaji", example = "12")
    private Integer idKaryawan;

    @NotNull(message = "periode wajib diisi")
    @Schema(description = "Awal bulan periode gaji dalam format ISO yyyy-MM-dd", example = "2026-08-01")
    private LocalDate periode;

    @PositiveOrZero(message = "gajiPokok tidak boleh negatif")
    @Schema(description = "Gaji pokok sebelum tunjangan dan potongan", example = "8500000.00")
    private BigDecimal gajiPokok;

    @PositiveOrZero(message = "tunjangan tidak boleh negatif")
    @Schema(description = "Total tunjangan", example = "1250000.00")
    private BigDecimal tunjangan;

    @PositiveOrZero(message = "potongan tidak boleh negatif")
    @Schema(description = "Total potongan", example = "450000.00")
    private BigDecimal potongan;
}
