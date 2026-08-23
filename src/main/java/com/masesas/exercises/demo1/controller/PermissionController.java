package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.BaseApiResponse;
import com.masesas.exercises.demo1.dto.PermissionRequest;
import com.masesas.exercises.demo1.dto.PermissionResponse;
import com.masesas.exercises.demo1.service.PermissionService;
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
@RequestMapping("/api/permission")
@RequiredArgsConstructor
@Tag(name = "Permission", description = "Katalog izin berbutir halus. Kode izin dipakai apa adanya "
        + "di hasAuthority(...) pada endpoint lain.")
@SecurityRequirement(name = "karyawanAuth")
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PERMISSION_WRITE')")
    @Operation(summary = "Tambah permission baru")
    @ApiResponse(responseCode = "201", description = "Permission dibuat")
    public BaseApiResponse<PermissionResponse> create(@Valid @RequestBody PermissionRequest request) {
        return BaseApiResponse.created("Permission dibuat", permissionService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    @Operation(summary = "Daftar permission")
    @ApiResponse(responseCode = "200", description = "Satu halaman permission")
    public BaseApiResponse<List<PermissionResponse>> findAll(Pageable pageable) {
        return BaseApiResponse.page("Satu halaman permission", permissionService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    @Operation(summary = "Ambil satu permission")
    @ApiResponse(responseCode = "200", description = "Permission ditemukan")
    public BaseApiResponse<PermissionResponse> findById(
            @Parameter(description = "ID permission", example = "13") @PathVariable Integer id) {
        return BaseApiResponse.ok("Permission ditemukan", permissionService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_WRITE')")
    @Operation(summary = "Ubah permission")
    @ApiResponse(responseCode = "200", description = "Permission diperbarui")
    public BaseApiResponse<PermissionResponse> update(
            @Parameter(description = "ID permission", example = "13") @PathVariable Integer id,
            @Valid @RequestBody PermissionRequest request) {
        return BaseApiResponse.ok("Permission diperbarui", permissionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PERMISSION_WRITE')")
    @Operation(summary = "Hapus permission",
            description = "Soft delete. Ditolak bila masih dipegang salah satu peran.")
    @ApiResponse(responseCode = "204", description = "Permission dihapus")
    public void delete(
            @Parameter(description = "ID permission", example = "13") @PathVariable Integer id) {
        permissionService.delete(id);
    }
}
