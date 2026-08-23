package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.BaseApiResponse;
import com.masesas.exercises.demo1.dto.RolePermissionRequest;
import com.masesas.exercises.demo1.dto.RolePermissionResponse;
import com.masesas.exercises.demo1.service.RolePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/role-permission")
@RequiredArgsConstructor
@Tag(name = "RolePermission", description = "Pemetaan peran ke izin. Perubahan di sini langsung "
        + "berlaku pada permintaan berikutnya karena pengguna dimuat ulang tiap request.")
@SecurityRequirement(name = "karyawanAuth")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_PERMISSION_READ')")
    @Operation(summary = "Seluruh pemetaan peran ke izin")
    @ApiResponse(responseCode = "200", description = "Daftar pemetaan")
    public BaseApiResponse<List<RolePermissionResponse>> findAll() {
        return BaseApiResponse.ok("Daftar pemetaan", rolePermissionService.findAll());
    }

    @GetMapping("/role/{idRole}")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION_READ')")
    @Operation(summary = "Izin yang dipegang satu peran")
    @ApiResponse(responseCode = "200", description = "Daftar izin peran tersebut")
    public BaseApiResponse<List<RolePermissionResponse>> findByRole(
            @Parameter(description = "ID peran", example = "2") @PathVariable Integer idRole) {
        return BaseApiResponse.ok("Daftar izin peran tersebut", rolePermissionService.findByRole(idRole));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_PERMISSION_WRITE')")
    @Operation(summary = "Berikan satu izin kepada satu peran")
    @ApiResponse(responseCode = "201", description = "Izin diberikan")
    public BaseApiResponse<RolePermissionResponse> grant(
            @Valid @RequestBody RolePermissionRequest request) {
        return BaseApiResponse.created("Izin diberikan", rolePermissionService.grant(request));
    }

    @DeleteMapping("/role/{idRole}/permission/{idPermission}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROLE_PERMISSION_WRITE')")
    @Operation(summary = "Cabut satu izin dari satu peran")
    @ApiResponse(responseCode = "204", description = "Izin dicabut")
    public void revoke(
            @Parameter(description = "ID peran", example = "2") @PathVariable Integer idRole,
            @Parameter(description = "ID izin", example = "13") @PathVariable Integer idPermission) {
        rolePermissionService.revoke(idRole, idPermission);
    }
}
