package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.BaseApiResponse;
import com.masesas.exercises.demo1.dto.EndpointAksesResponse;
import com.masesas.exercises.demo1.dto.RoleAksesResponse;
import com.masesas.exercises.demo1.service.RoleMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rolemap")
@RequiredArgsConstructor
@Tag(name = "RoleMap", description = "Peta peran terhadap endpoint, dibaca langsung dari "
        + "anotasi @PreAuthorize saat aplikasi berjalan. Terbuka tanpa token.")
public class RoleMapController {

    private final RoleMapService roleMapService;

    @GetMapping
    @Operation(
            summary = "Daftar seluruh endpoint beserta peran yang diizinkan",
            description = "Ikut menandai endpoint mana yang publik.")
    @ApiResponse(responseCode = "200", description = "Daftar endpoint")
    public BaseApiResponse<List<EndpointAksesResponse>> semua() {
        return BaseApiResponse.ok("Daftar endpoint", roleMapService.semua());
    }

    @GetMapping("/matriks")
    @Operation(
            summary = "Matriks akses untuk semua peran",
            description = "Satu entri per peran, masing-masing berisi endpoint yang bisa diaksesnya.")
    @ApiResponse(responseCode = "200", description = "Matriks akses")
    public BaseApiResponse<List<RoleAksesResponse>> matriks() {
        return BaseApiResponse.ok("Matriks akses", roleMapService.semuaPeran());
    }

    @GetMapping("/{peran}")
    @Operation(summary = "Endpoint yang bisa diakses satu peran tertentu")
    @ApiResponse(responseCode = "200", description = "Daftar endpoint untuk peran tersebut")
    public BaseApiResponse<RoleAksesResponse> perPeran(
            @Parameter(description = "Nama peran, huruf besar-kecil bebas", example = "ADMIN")
            @PathVariable String peran) {
        return BaseApiResponse.ok("Daftar endpoint untuk peran tersebut", roleMapService.untukPeran(peran));
    }
}
