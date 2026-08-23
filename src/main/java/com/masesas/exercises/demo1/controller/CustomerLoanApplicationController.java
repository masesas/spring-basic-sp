package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.BaseApiResponse;
import com.masesas.exercises.demo1.dto.LoanApplicationRequest;
import com.masesas.exercises.demo1.dto.LoanApplicationResponse;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/loan-application")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "CustomerLoanApplication", description = "Pengajuan pinjaman milik customer yang sedang "
        + "login. Pemilik pengajuan diambil dari token, tidak pernah dari badan permintaan, "
        + "sehingga satu customer tidak bisa menyentuh pengajuan customer lain.")
@SecurityRequirement(name = "customerAuth")
public class CustomerLoanApplicationController {

    private static final String ID_PENGAJUAN = "ID pengajuan";

    private final LoanApplicationService loanApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Buat pengajuan baru berstatus DRAFT")
    @ApiResponse(responseCode = "201", description = "Pengajuan dibuat")
    public BaseApiResponse<LoanApplicationResponse> create(
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody LoanApplicationRequest request) {
        return BaseApiResponse.created(
                "Pengajuan dibuat", loanApplicationService.buatDraft(user.getUsername(), request));
    }

    @GetMapping
    @Operation(summary = "Daftar pengajuan milik sendiri")
    @ApiResponse(responseCode = "200", description = "Satu halaman pengajuan")
    public BaseApiResponse<List<LoanApplicationResponse>> findAll(
            @AuthenticationPrincipal AppUser user, Pageable pageable) {
        return BaseApiResponse.page(
                "Satu halaman pengajuan",
                loanApplicationService.daftarMilikCustomer(user.getUsername(), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ambil satu pengajuan milik sendiri")
    @ApiResponse(responseCode = "200", description = "Pengajuan ditemukan")
    public BaseApiResponse<LoanApplicationResponse> findById(
            @AuthenticationPrincipal AppUser user,
            @Parameter(description = ID_PENGAJUAN, example = "1") @PathVariable Integer id) {
        return BaseApiResponse.ok(
                "Pengajuan ditemukan",
                loanApplicationService.detailMilikCustomer(user.getUsername(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Revisi pengajuan", description = "Hanya pengajuan berstatus DRAFT.")
    @ApiResponse(responseCode = "200", description = "Pengajuan diperbarui")
    public BaseApiResponse<LoanApplicationResponse> update(
            @AuthenticationPrincipal AppUser user,
            @Parameter(description = ID_PENGAJUAN, example = "1") @PathVariable Integer id,
            @Valid @RequestBody LoanApplicationRequest request) {
        return BaseApiResponse.ok(
                "Pengajuan diperbarui",
                loanApplicationService.revisiDraft(user.getUsername(), id, request));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Kirim pengajuan untuk diproses",
            description = "DRAFT menjadi SUBMITTED. Setelah ini pengajuan tidak bisa direvisi lagi.")
    @ApiResponse(responseCode = "200", description = "Pengajuan dikirim")
    public BaseApiResponse<LoanApplicationResponse> submit(
            @AuthenticationPrincipal AppUser user,
            @Parameter(description = ID_PENGAJUAN, example = "1") @PathVariable Integer id) {
        return BaseApiResponse.ok(
                "Pengajuan dikirim", loanApplicationService.ajukan(user.getUsername(), id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Batalkan pengajuan",
            description = "Hanya pengajuan berstatus DRAFT atau SUBMITTED.")
    @ApiResponse(responseCode = "200", description = "Pengajuan dibatalkan")
    public BaseApiResponse<LoanApplicationResponse> cancel(
            @AuthenticationPrincipal AppUser user,
            @Parameter(description = ID_PENGAJUAN, example = "1") @PathVariable Integer id) {
        return BaseApiResponse.ok(
                "Pengajuan dibatalkan", loanApplicationService.batalkan(user.getUsername(), id));
    }
}
