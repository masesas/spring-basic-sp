package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.PayrollRequest;
import com.masesas.exercises.demo1.dto.PayrollResponse;
import com.masesas.exercises.demo1.dto.PayrollUpdateRequest;
import com.masesas.exercises.demo1.service.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST endpoint untuk slip gaji.
 *
 * <p>Karena primary key-nya komposit, satu baris dialamati oleh dua segmen path:
 * {@code /api/payroll/{idKaryawan}/{periode}} — bukan satu {@code id} seperti
 * resource lain di project ini.
 */
@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    /** POST /api/payroll — buat slip gaji baru. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('HR')")
    public PayrollResponse create(@Valid @RequestBody PayrollRequest request) {
        return payrollService.create(request);
    }

    /** GET /api/payroll/{idKaryawan}/{periode} — ambil satu slip gaji, misal .../12/2026-08-01 */
    @GetMapping("/{idKaryawan}/{periode}")
    public PayrollResponse findById(
            @PathVariable Integer idKaryawan,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periode) {
        return payrollService.findById(idKaryawan, periode);
    }

    /** GET /api/payroll?page=0&size=10 — semua slip gaji. */
    @GetMapping
    public Page<PayrollResponse> findAll(Pageable pageable) {
        return payrollService.findAll(pageable);
    }

    /** GET /api/payroll/periode/{periode} — semua slip gaji pada satu periode. */
    @GetMapping("/periode/{periode}")
    public Page<PayrollResponse> findByPeriode(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periode,
            Pageable pageable) {
        return payrollService.findByPeriode(periode, pageable);
    }

    /** GET /api/payroll/karyawan/{idKaryawan} — riwayat gaji satu karyawan. */
    @GetMapping("/karyawan/{idKaryawan}")
    public List<PayrollResponse> findRiwayatKaryawan(@PathVariable Integer idKaryawan) {
        return payrollService.findRiwayatKaryawan(idKaryawan);
    }

    /** GET /api/payroll/rekap?periode=2026-08-01 — total gaji bersih pada satu periode. */
    @GetMapping("/rekap")
    public Map<String, Object> rekap(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periode) {
        BigDecimal total = payrollService.totalBersihPadaPeriode(periode);
        return Map.of("periode", periode.withDayOfMonth(1), "totalBersih", total);
    }

    /** PUT /api/payroll/{idKaryawan}/{periode} — revisi nominal slip gaji. */
    @PutMapping("/{idKaryawan}/{periode}")
    @PreAuthorize("hasRole('HR')")
    public PayrollResponse update(
            @PathVariable Integer idKaryawan,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periode,
            @Valid @RequestBody PayrollUpdateRequest request) {
        return payrollService.update(idKaryawan, periode, request);
    }

    /** POST /api/payroll/{idKaryawan}/{periode}/approve — kunci slip gaji. */
    @PostMapping("/{idKaryawan}/{periode}/approve")
    @PreAuthorize("hasRole('HR')")
    public PayrollResponse approve(
            @PathVariable Integer idKaryawan,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periode) {
        return payrollService.approve(idKaryawan, periode);
    }

    /** DELETE /api/payroll/{idKaryawan}/{periode} — hapus slip gaji (hard delete). */
    @DeleteMapping("/{idKaryawan}/{periode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('HR')")
    public void delete(
            @PathVariable Integer idKaryawan,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periode) {
        payrollService.delete(idKaryawan, periode);
    }
}
