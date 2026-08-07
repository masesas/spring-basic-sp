package com.masesas.exercises.demo1.dto;

import java.math.BigDecimal;

/**
 * Body untuk merevisi slip gaji. Primary key ({@code idKaryawan} + {@code periode})
 * diambil dari path URL dan tidak pernah bisa diubah — itu identitas barisnya.
 */
public record PayrollUpdateRequest(
        BigDecimal gajiPokok,
        BigDecimal tunjangan,
        BigDecimal potongan) {
}
