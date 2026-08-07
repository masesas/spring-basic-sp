package com.masesas.exercises.demo1.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body untuk membuat slip gaji.
 *
 * <p>{@code periode} boleh dikirim sebagai tanggal berapa pun dalam bulan yang dimaksud
 * (misal {@code 2026-08-17}); service akan menormalkannya ke tanggal 1.
 */
public record PayrollRequest(
        Integer idKaryawan,
        LocalDate periode,
        BigDecimal gajiPokok,
        BigDecimal tunjangan,
        BigDecimal potongan) {
}
