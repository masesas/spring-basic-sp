package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.KomponenGaji;
import com.masesas.exercises.demo1.entity.PayrollKaryawan;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Slip gaji sebagaimana dikembalikan API. {@code bruto} dan {@code bersih} adalah
 * nilai turunan yang dihitung di {@link KomponenGaji}, bukan kolom di database.
 */
public record PayrollResponse(
        Integer idKaryawan,
        String namaKaryawan,
        LocalDate periode,
        BigDecimal gajiPokok,
        BigDecimal tunjangan,
        BigDecimal potongan,
        BigDecimal bruto,
        BigDecimal bersih,
        Instant createdDate,
        Instant updatedDate) {

    public static PayrollResponse from(PayrollKaryawan payroll) {
        KomponenGaji komponen = payroll.getKomponen();
        return new PayrollResponse(
                payroll.getKaryawan().getId(),
                payroll.getKaryawan().getNama(),
                payroll.getPeriode(),
                komponen.getGajiPokok(),
                komponen.getTunjangan(),
                komponen.getPotongan(),
                komponen.bruto(),
                komponen.bersih(),
                payroll.getCreatedDate(),
                payroll.getUpdatedDate());
    }
}
