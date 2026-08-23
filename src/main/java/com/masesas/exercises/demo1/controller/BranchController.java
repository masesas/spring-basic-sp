package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.BaseApiResponse;
import com.masesas.exercises.demo1.dto.BranchRequest;
import com.masesas.exercises.demo1.dto.BranchResponse;
import com.masesas.exercises.demo1.service.BranchService;
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
@RequestMapping("/api/branch")
@RequiredArgsConstructor
@Tag(name = "Branch", description = "Master cabang. Membaca butuh permission BRANCH_READ, "
        + "mengubah butuh BRANCH_WRITE.")
@SecurityRequirement(name = "karyawanAuth")
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('BRANCH_WRITE')")
    @Operation(summary = "Tambah cabang baru")
    @ApiResponse(responseCode = "201", description = "Cabang dibuat")
    public BaseApiResponse<BranchResponse> create(@Valid @RequestBody BranchRequest request) {
        return BaseApiResponse.created("Cabang dibuat", branchService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BRANCH_READ')")
    @Operation(summary = "Daftar cabang",
            description = "Memakai parameter paging standar Spring Data: page, size, sort.")
    @ApiResponse(responseCode = "200", description = "Satu halaman cabang")
    public BaseApiResponse<List<BranchResponse>> findAll(Pageable pageable) {
        return BaseApiResponse.page("Satu halaman cabang", branchService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BRANCH_READ')")
    @Operation(summary = "Ambil satu cabang")
    @ApiResponse(responseCode = "200", description = "Cabang ditemukan")
    public BaseApiResponse<BranchResponse> findById(
            @Parameter(description = "ID cabang", example = "1") @PathVariable Integer id) {
        return BaseApiResponse.ok("Cabang ditemukan", branchService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BRANCH_WRITE')")
    @Operation(summary = "Ubah cabang")
    @ApiResponse(responseCode = "200", description = "Cabang diperbarui")
    public BaseApiResponse<BranchResponse> update(
            @Parameter(description = "ID cabang", example = "1") @PathVariable Integer id,
            @Valid @RequestBody BranchRequest request) {
        return BaseApiResponse.ok("Cabang diperbarui", branchService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('BRANCH_WRITE')")
    @Operation(summary = "Hapus cabang",
            description = "Soft delete. Ditolak bila cabang masih dipakai pengajuan pinjaman.")
    @ApiResponse(responseCode = "204", description = "Cabang dihapus")
    public void delete(
            @Parameter(description = "ID cabang", example = "1") @PathVariable Integer id) {
        branchService.delete(id);
    }
}
