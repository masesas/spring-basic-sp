package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Slip gaji beserta nilai turunan dan jejak auditnya")
public class PayrollResponse {

    @Schema(description = "ID karyawan penerima gaji", example = "12")
    private Integer idKaryawan;
    @Schema(description = "Nama karyawan penerima gaji", example = "Budi Santoso")
    private String namaKaryawan;
    @Schema(description = "Awal bulan periode gaji", example = "2026-08-01")
    private LocalDate periode;
    @Schema(description = "Gaji pokok sebelum tunjangan dan potongan", example = "8500000.00")
    private BigDecimal gajiPokok;
    @Schema(description = "Total tunjangan", example = "1250000.00")
    private BigDecimal tunjangan;
    @Schema(description = "Total potongan", example = "450000.00")
    private BigDecimal potongan;
    @Schema(description = "Gaji pokok ditambah tunjangan", example = "9750000.00")
    private BigDecimal bruto;
    @Schema(description = "Bruto dikurangi potongan", example = "9300000.00")
    private BigDecimal bersih;
    @Schema(description = "Status slip gaji")
    private StatusPayroll status;
    @Schema(description = "Nomor versi baris, dikirim balik saat revisi", example = "0")
    private Long version;
    @Schema(description = "Waktu pembuatan baris", example = "2026-08-17T16:15:02.902570Z")
    private Instant createdDate;
    @Schema(description = "Waktu perubahan terakhir", example = "2026-08-17T16:15:02.902570Z")
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
