package com.masesas.exercises.demo1.dto;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Body untuk merevisi slip gaji. Primary key ({@code idKaryawan} + {@code periode})
 * diambil dari path URL dan tidak pernah bisa diubah — itu identitas barisnya.
 */
public record PayrollUpdateRequest(
        Long version,
        @PositiveOrZero(message = "gajiPokok tidak boleh negatif") BigDecimal gajiPokok,
        @PositiveOrZero(message = "tunjangan tidak boleh negatif") BigDecimal tunjangan,
        @PositiveOrZero(message = "potongan tidak boleh negatif") BigDecimal potongan) {
}
