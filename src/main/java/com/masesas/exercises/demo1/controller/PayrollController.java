package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.PayrollRequest;
import com.masesas.exercises.demo1.dto.PayrollResponse;
import com.masesas.exercises.demo1.dto.PayrollUpdateRequest;
import com.masesas.exercises.demo1.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','HR')")
@Tag(name = "Payroll", description = "Slip gaji. Primary key-nya komposit, jadi satu baris "
        + "dialamati dua segmen path: /api/payroll/{idKaryawan}/{periode} — bukan satu id "
        + "seperti resource lain. Butuh peran ADMIN, MANAGER, atau HR.")
@SecurityRequirement(name = "karyawanAuth")
public class PayrollController {

    private static final String PERIODE = "Awal bulan periode gaji dalam format ISO yyyy-MM-dd";

    private final PayrollService payrollService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Buat slip gaji baru",
            description = "Satu karyawan hanya boleh punya satu slip per periode.")
    @ApiResponse(responseCode = "201", description = "Slip gaji dibuat")
    public PayrollResponse create(@Valid @RequestBody PayrollRequest request) {
        return payrollService.create(request);
    }

    @GetMapping("/{idKaryawan}/{periode}")
    @Operation(summary = "Ambil satu slip gaji")
    @ApiResponse(responseCode = "200", description = "Slip gaji ditemukan")
    public PayrollResponse findById(
            @Parameter(description = "ID karyawan", example = "12")
            @PathVariable Integer idKaryawan,
            @Parameter(description = PERIODE, example = "2026-08-01")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periode) {
        return payrollService.findById(idKaryawan, periode);
    }

    @GetMapping
    @Operation(
            summary = "Daftar seluruh slip gaji",
            description = "Memakai parameter paging standar Spring Data: page, size, sort.")
    @ApiResponse(responseCode = "200", description = "Satu halaman slip gaji")
    public Page<PayrollResponse> findAll(Pageable pageable) {
        return payrollService.findAll(pageable);
    }

    @GetMapping("/periode/{periode}")
    @Operation(summary = "Daftar slip gaji pada satu periode")
    @ApiResponse(responseCode = "200", description = "Satu halaman slip gaji pada periode itu")
    public Page<PayrollResponse> findByPeriode(
            @Parameter(description = PERIODE, example = "2026-08-01")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periode,
            Pageable pageable) {
        return payrollService.findByPeriode(periode, pageable);
    }

    @GetMapping("/karyawan/{idKaryawan}")
    @Operation(
            summary = "Riwayat gaji satu karyawan",
            description = "Seluruh periode untuk karyawan tersebut, tanpa paging.")
    @ApiResponse(responseCode = "200", description = "Riwayat slip gaji")
    public List<PayrollResponse> findRiwayatKaryawan(
            @Parameter(description = "ID karyawan", example = "12")
            @PathVariable Integer idKaryawan) {
        return payrollService.findRiwayatKaryawan(idKaryawan);
    }

    @GetMapping("/rekap")
    @Operation(
            summary = "Total gaji bersih pada satu periode",
            description = "Balasannya berisi periode yang dinormalkan ke awal bulan "
                    + "dan totalBersih sebagai jumlah seluruh slip pada periode itu.")
    @ApiResponse(responseCode = "200", description = "Rekap total gaji bersih")
    public Map<String, Object> rekap(
            @Parameter(description = PERIODE, example = "2026-08-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periode) {
        BigDecimal total = payrollService.totalBersihPadaPeriode(periode);
        return Map.of("periode", periode.withDayOfMonth(1), "totalBersih", total);
    }

    @PutMapping("/{idKaryawan}/{periode}")
    @Operation(
            summary = "Revisi nominal slip gaji",
            description = "Slip yang sudah disetujui tidak bisa direvisi lagi.")
    @ApiResponse(responseCode = "200", description = "Slip gaji diperbarui")
    public PayrollResponse update(
            @Parameter(description = "ID karyawan", example = "12")
            @PathVariable Integer idKaryawan,
            @Parameter(description = PERIODE, example = "2026-08-01")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periode,
            @Valid @RequestBody PayrollUpdateRequest request) {
        return payrollService.update(idKaryawan, periode, request);
    }

    @PostMapping("/{idKaryawan}/{periode}/approve")
    @Operation(
            summary = "Setujui dan kunci slip gaji",
            description = "Setelah disetujui, nominalnya tidak bisa diubah lagi.")
    @ApiResponse(responseCode = "200", description = "Slip gaji disetujui")
    public PayrollResponse approve(
            @Parameter(description = "ID karyawan", example = "12")
            @PathVariable Integer idKaryawan,
            @Parameter(description = PERIODE, example = "2026-08-01")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periode) {
        return payrollService.approve(idKaryawan, periode);
    }

    @DeleteMapping("/{idKaryawan}/{periode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(
            summary = "Hapus slip gaji",
            description = "Butuh peran ADMIN atau HR. Penghapusan permanen, bukan soft delete.")
    @ApiResponse(responseCode = "204", description = "Slip gaji dihapus")
    public void delete(
            @Parameter(description = "ID karyawan", example = "12")
            @PathVariable Integer idKaryawan,
            @Parameter(description = PERIODE, example = "2026-08-01")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periode) {
        payrollService.delete(idKaryawan, periode);
    }
}
