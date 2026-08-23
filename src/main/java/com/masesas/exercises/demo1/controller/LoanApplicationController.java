package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.BaseApiResponse;
import com.masesas.exercises.demo1.dto.LoanApplicationDecisionRequest;
import com.masesas.exercises.demo1.dto.LoanApplicationResponse;
import com.masesas.exercises.demo1.entity.StatusLoanApplication;
import com.masesas.exercises.demo1.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loan-application")
@RequiredArgsConstructor
@Tag(name = "LoanApplication", description = "Pemrosesan pengajuan pinjaman oleh pegawai. "
        + "Tiap transisi status dijaga permission tersendiri: LOAN_APPLICATION_APPROVE, "
        + "LOAN_APPLICATION_REJECT, dan LOAN_APPLICATION_DISBURSE. Pengajuannya sendiri dibuat "
        + "customer lewat /api/customer/loan-application.")
@SecurityRequirement(name = "karyawanAuth")
public class LoanApplicationController {

    private static final String ID_PENGAJUAN = "ID pengajuan";

    private final LoanApplicationService loanApplicationService;

    @GetMapping
    @PreAuthorize("hasAuthority('LOAN_APPLICATION_READ')")
    @Operation(summary = "Daftar seluruh pengajuan pinjaman",
            description = "Bisa disaring per status. Memakai parameter paging standar Spring Data.")
    @ApiResponse(responseCode = "200", description = "Satu halaman pengajuan")
    public BaseApiResponse<List<LoanApplicationResponse>> findAll(
            @Parameter(description = "Saring per status, kosongkan untuk semua", example = "SUBMITTED")
            @RequestParam(required = false) StatusLoanApplication status,
            Pageable pageable) {
        return BaseApiResponse.page(
                "Satu halaman pengajuan", loanApplicationService.daftar(status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LOAN_APPLICATION_READ')")
    @Operation(summary = "Ambil satu pengajuan pinjaman")
    @ApiResponse(responseCode = "200", description = "Pengajuan ditemukan")
    public BaseApiResponse<LoanApplicationResponse> findById(
            @Parameter(description = ID_PENGAJUAN, example = "1") @PathVariable Integer id) {
        return BaseApiResponse.ok("Pengajuan ditemukan", loanApplicationService.detail(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('LOAN_APPLICATION_APPROVE')")
    @Operation(summary = "Setujui pengajuan",
            description = "Hanya dari status SUBMITTED. Ditolak bila jumlah pengajuan melebihi "
                    + "sisa plafond customer.")
    @ApiResponse(responseCode = "200", description = "Pengajuan disetujui")
    public BaseApiResponse<LoanApplicationResponse> approve(
            @Parameter(description = ID_PENGAJUAN, example = "1") @PathVariable Integer id,
            @Valid @RequestBody(required = false) LoanApplicationDecisionRequest request) {
        String catatan = request == null ? null : request.getCatatan();
        return BaseApiResponse.ok("Pengajuan disetujui", loanApplicationService.setujui(id, catatan));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('LOAN_APPLICATION_REJECT')")
    @Operation(summary = "Tolak pengajuan",
            description = "Hanya dari status SUBMITTED. Catatan alasan wajib diisi.")
    @ApiResponse(responseCode = "200", description = "Pengajuan ditolak")
    public BaseApiResponse<LoanApplicationResponse> reject(
            @Parameter(description = ID_PENGAJUAN, example = "1") @PathVariable Integer id,
            @Valid @RequestBody LoanApplicationDecisionRequest request) {
        return BaseApiResponse.ok(
                "Pengajuan ditolak", loanApplicationService.tolak(id, request.getCatatan()));
    }

    @PostMapping("/{id}/disburse")
    @PreAuthorize("hasAuthority('LOAN_APPLICATION_DISBURSE')")
    @Operation(summary = "Cairkan pengajuan",
            description = "Hanya dari status APPROVED. Menambah plafond terpakai customer "
                    + "sebesar nilai pinjaman.")
    @ApiResponse(responseCode = "200", description = "Pengajuan dicairkan")
    public BaseApiResponse<LoanApplicationResponse> disburse(
            @Parameter(description = ID_PENGAJUAN, example = "1") @PathVariable Integer id) {
        return BaseApiResponse.ok("Pengajuan dicairkan", loanApplicationService.cairkan(id));
    }
}
