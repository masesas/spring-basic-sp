package com.masesas.exercises.demo1.dto;

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
public class PayrollRequest {

    @NotNull(message = "idKaryawan wajib diisi")
    private Integer idKaryawan;

    @NotNull(message = "periode wajib diisi")
    private LocalDate periode;

    @PositiveOrZero(message = "gajiPokok tidak boleh negatif")
    private BigDecimal gajiPokok;

    @PositiveOrZero(message = "tunjangan tidak boleh negatif")
    private BigDecimal tunjangan;

    @PositiveOrZero(message = "potongan tidak boleh negatif")
    private BigDecimal potongan;
}
