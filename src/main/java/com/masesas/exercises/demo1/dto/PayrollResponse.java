package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.KomponenGaji;
import com.masesas.exercises.demo1.entity.PayrollKaryawan;
import com.masesas.exercises.demo1.entity.StatusPayroll;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayrollResponse {

    private Integer idKaryawan;
    private String namaKaryawan;
    private LocalDate periode;
    private BigDecimal gajiPokok;
    private BigDecimal tunjangan;
    private BigDecimal potongan;
    private BigDecimal bruto;
    private BigDecimal bersih;
    private StatusPayroll status;
    private Long version;
    private Instant createdDate;
    private Instant updatedDate;

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
                payroll.getStatus(),
                payroll.getVersion(),
                payroll.getCreatedDate(),
                payroll.getUpdatedDate());
    }
}
