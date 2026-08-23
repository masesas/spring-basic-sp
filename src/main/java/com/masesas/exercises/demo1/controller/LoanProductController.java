package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.BaseApiResponse;
import com.masesas.exercises.demo1.dto.LoanProductRequest;
import com.masesas.exercises.demo1.dto.LoanProductResponse;
import com.masesas.exercises.demo1.service.LoanProductService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/loan-product")
@RequiredArgsConstructor
@Tag(name = "LoanProduct", description = "Master produk pinjaman: bunga, rentang tenor, dan "
        + "rentang nominal yang boleh diajukan.")
@SecurityRequirement(name = "karyawanAuth")
public class LoanProductController {

    private final LoanProductService loanProductService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOAN_PRODUCT_WRITE')")
    @Operation(summary = "Tambah produk pinjaman")
    @ApiResponse(responseCode = "201", description = "Produk pinjaman dibuat")
    public BaseApiResponse<LoanProductResponse> create(@Valid @RequestBody LoanProductRequest request) {
        return BaseApiResponse.created("Produk pinjaman dibuat", loanProductService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LOAN_PRODUCT_READ')")
    @Operation(summary = "Daftar produk pinjaman")
    @ApiResponse(responseCode = "200", description = "Satu halaman produk pinjaman")
    public BaseApiResponse<List<LoanProductResponse>> findAll(Pageable pageable) {
        return BaseApiResponse.page("Satu halaman produk pinjaman", loanProductService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LOAN_PRODUCT_READ')")
    @Operation(summary = "Ambil satu produk pinjaman")
    @ApiResponse(responseCode = "200", description = "Produk pinjaman ditemukan")
    public BaseApiResponse<LoanProductResponse> findById(
            @Parameter(description = "ID produk", example = "1") @PathVariable Integer id) {
        return BaseApiResponse.ok("Produk pinjaman ditemukan", loanProductService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LOAN_PRODUCT_WRITE')")
    @Operation(summary = "Ubah produk pinjaman")
    @ApiResponse(responseCode = "200", description = "Produk pinjaman diperbarui")
    public BaseApiResponse<LoanProductResponse> update(
            @Parameter(description = "ID produk", example = "1") @PathVariable Integer id,
            @Valid @RequestBody LoanProductRequest request) {
        return BaseApiResponse.ok("Produk pinjaman diperbarui", loanProductService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('LOAN_PRODUCT_WRITE')")
    @Operation(summary = "Hapus produk pinjaman",
            description = "Soft delete. Ditolak bila masih dipakai pengajuan yang belum selesai.")
    @ApiResponse(responseCode = "204", description = "Produk pinjaman dihapus")
    public void delete(
            @Parameter(description = "ID produk", example = "1") @PathVariable Integer id) {
        loanProductService.delete(id);
    }
}
