package com.masesas.exercises.demo1.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body untuk membuat slip gaji.
 *
 * <p>{@code periode} boleh dikirim sebagai tanggal berapa pun dalam bulan yang dimaksud
 * (misal {@code 2026-08-17}); service akan menormalkannya ke tanggal 1.
 */
public record PayrollRequest(
        @NotNull(message = "idKaryawan wajib diisi") Integer idKaryawan,
        @NotNull(message = "periode wajib diisi") LocalDate periode,
        @PositiveOrZero(message = "gajiPokok tidak boleh negatif") BigDecimal gajiPokok,
        @PositiveOrZero(message = "tunjangan tidak boleh negatif") BigDecimal tunjangan,
        @PositiveOrZero(message = "potongan tidak boleh negatif") BigDecimal potongan) {
}
