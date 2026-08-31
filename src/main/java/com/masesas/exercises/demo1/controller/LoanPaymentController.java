package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.BaseApiResponse;
import com.masesas.exercises.demo1.dto.LoanPaymentRequest;
import com.masesas.exercises.demo1.dto.LoanPaymentResponse;
import com.masesas.exercises.demo1.dto.LoanPaymentTotalResponse;
import com.masesas.exercises.demo1.service.LoanPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loan-payment")
@RequiredArgsConstructor
@Tag(name = "LoanPayment", description = "Pembayaran angsuran atas pinjaman yang sudah dicairkan. "
        + "Tiap pembayaran mengurangi plafond terpakai customer.")
@SecurityRequirement(name = "karyawanAuth")
public class LoanPaymentController {

    private static final String ID_PENGAJUAN = "ID pengajuan pinjaman";

    private final LoanPaymentService loanPaymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOAN_PAYMENT_WRITE')")
    @Operation(summary = "Catat pembayaran angsuran",
            description = "Pinjaman harus berstatus DISBURSED dan nomor angsuran belum pernah dicatat.")
    @ApiResponse(responseCode = "201", description = "Pembayaran dicatat")
    public BaseApiResponse<LoanPaymentResponse> catat(@Valid @RequestBody LoanPaymentRequest request) {
        return BaseApiResponse.created("Pembayaran dicatat", loanPaymentService.catat(request));
    }

    @GetMapping("/application/{idLoanApplication}")
    @PreAuthorize("hasAuthority('LOAN_PAYMENT_READ')")
    @Operation(summary = "Riwayat pembayaran satu pinjaman")
    @ApiResponse(responseCode = "200", description = "Riwayat pembayaran")
    public BaseApiResponse<List<LoanPaymentResponse>> daftar(
            @Parameter(description = ID_PENGAJUAN, example = "1") @PathVariable Integer idLoanApplication) {
        return BaseApiResponse.ok(
                "Riwayat pembayaran", loanPaymentService.daftarPerPengajuan(idLoanApplication));
    }

    @GetMapping("/application/{idLoanApplication}/total")
    @PreAuthorize("hasAuthority('LOAN_PAYMENT_READ')")
    @Operation(summary = "Total yang sudah dibayar untuk satu pinjaman")
    @ApiResponse(responseCode = "200", description = "Total pembayaran")
    public BaseApiResponse<LoanPaymentTotalResponse> total(
            @Parameter(description = ID_PENGAJUAN, example = "1") @PathVariable Integer idLoanApplication) {
        return BaseApiResponse.ok(
                "Total pembayaran",
                new LoanPaymentTotalResponse(loanPaymentService.totalDibayar(idLoanApplication)));
    }
}
