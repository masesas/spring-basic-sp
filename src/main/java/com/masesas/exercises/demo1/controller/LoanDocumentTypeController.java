package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.BaseApiResponse;
import com.masesas.exercises.demo1.dto.LoanDocumentTypeRequest;
import com.masesas.exercises.demo1.dto.LoanDocumentTypeResponse;
import com.masesas.exercises.demo1.service.LoanDocumentTypeService;
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
@RequestMapping("/api/loan-document-type")
@RequiredArgsConstructor
@Tag(name = "LoanDocumentType", description = "Master jenis dokumen yang menyertai pengajuan pinjaman.")
@SecurityRequirement(name = "karyawanAuth")
public class LoanDocumentTypeController {

    private final LoanDocumentTypeService loanDocumentTypeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOAN_DOCUMENT_TYPE_WRITE')")
    @Operation(summary = "Tambah jenis dokumen")
    @ApiResponse(responseCode = "201", description = "Jenis dokumen dibuat")
    public BaseApiResponse<LoanDocumentTypeResponse> create(
            @Valid @RequestBody LoanDocumentTypeRequest request) {
        return BaseApiResponse.created("Jenis dokumen dibuat", loanDocumentTypeService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LOAN_DOCUMENT_TYPE_READ')")
    @Operation(summary = "Daftar jenis dokumen")
    @ApiResponse(responseCode = "200", description = "Satu halaman jenis dokumen")
    public BaseApiResponse<List<LoanDocumentTypeResponse>> findAll(Pageable pageable) {
        return BaseApiResponse.page(
                "Satu halaman jenis dokumen", loanDocumentTypeService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LOAN_DOCUMENT_TYPE_READ')")
    @Operation(summary = "Ambil satu jenis dokumen")
    @ApiResponse(responseCode = "200", description = "Jenis dokumen ditemukan")
    public BaseApiResponse<LoanDocumentTypeResponse> findById(
            @Parameter(description = "ID jenis dokumen", example = "1") @PathVariable Integer id) {
        return BaseApiResponse.ok("Jenis dokumen ditemukan", loanDocumentTypeService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LOAN_DOCUMENT_TYPE_WRITE')")
    @Operation(summary = "Ubah jenis dokumen")
    @ApiResponse(responseCode = "200", description = "Jenis dokumen diperbarui")
    public BaseApiResponse<LoanDocumentTypeResponse> update(
            @Parameter(description = "ID jenis dokumen", example = "1") @PathVariable Integer id,
            @Valid @RequestBody LoanDocumentTypeRequest request) {
        return BaseApiResponse.ok(
                "Jenis dokumen diperbarui", loanDocumentTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('LOAN_DOCUMENT_TYPE_WRITE')")
    @Operation(summary = "Hapus jenis dokumen", description = "Soft delete.")
    @ApiResponse(responseCode = "204", description = "Jenis dokumen dihapus")
    public void delete(
            @Parameter(description = "ID jenis dokumen", example = "1") @PathVariable Integer id) {
        loanDocumentTypeService.delete(id);
    }
}
