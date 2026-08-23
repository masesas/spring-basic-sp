package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.BaseApiResponse;
import com.masesas.exercises.demo1.dto.LoanPlafondRequest;
import com.masesas.exercises.demo1.dto.LoanPlafondResponse;
import com.masesas.exercises.demo1.service.LoanPlafondService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loan-plafond")
@RequiredArgsConstructor
@Tag(name = "LoanPlafond", description = "Batas kredit per customer. Sisa plafond dihitung dari "
        + "total dikurangi terpakai; terpakai bertambah saat pencairan dan berkurang saat pembayaran.")
@SecurityRequirement(name = "karyawanAuth")
public class LoanPlafondController {

    private final LoanPlafondService loanPlafondService;

    @PutMapping
    @PreAuthorize("hasAuthority('LOAN_PLAFOND_WRITE')")
    @Operation(summary = "Tetapkan atau ubah plafond satu customer",
            description = "Membuat baris baru bila customer belum punya plafond, "
                    + "selain itu mengubah yang sudah ada.")
    @ApiResponse(responseCode = "200", description = "Plafond ditetapkan")
    public BaseApiResponse<LoanPlafondResponse> tetapkan(
            @Valid @RequestBody LoanPlafondRequest request) {
        return BaseApiResponse.ok("Plafond ditetapkan", loanPlafondService.tetapkan(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LOAN_PLAFOND_READ')")
    @Operation(summary = "Daftar plafond seluruh customer")
    @ApiResponse(responseCode = "200", description = "Satu halaman plafond")
    public BaseApiResponse<List<LoanPlafondResponse>> findAll(Pageable pageable) {
        return BaseApiResponse.page("Satu halaman plafond", loanPlafondService.findAll(pageable));
    }

    @GetMapping("/customer/{idCustomer}")
    @PreAuthorize("hasAuthority('LOAN_PLAFOND_READ')")
    @Operation(summary = "Plafond satu customer")
    @ApiResponse(responseCode = "200", description = "Plafond ditemukan")
    public BaseApiResponse<LoanPlafondResponse> findByCustomer(
            @Parameter(description = "ID customer", example = "1") @PathVariable Integer idCustomer) {
        return BaseApiResponse.ok("Plafond ditemukan", loanPlafondService.findByCustomer(idCustomer));
    }
}
