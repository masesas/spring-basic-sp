package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.model.StatistikKaryawan;
import com.masesas.exercises.demo1.service.KaryawanStatistikService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sp/karyawan/statistik")
@RequiredArgsConstructor
@Tag(name = "KaryawanStatistik", description = "Stored procedure ber-OUT parameter. "
        + "Butuh token, tanpa pembatasan peran.")
@SecurityRequirement(name = "karyawanAuth")
@SecurityRequirement(name = "customerAuth")
public class KaryawanStatistikController {

    private static final String STATUS = "Status kepegawaian yang dihitung";

    private final KaryawanStatistikService service;

    @GetMapping("/total")
    @Operation(
            summary = "Jumlah karyawan pada satu status",
            description = "Memanggil stored procedure dengan satu OUT parameter.")
    @ApiResponse(responseCode = "200", description = "Status yang diminta beserta jumlahnya")
    public Map<String, Object> total(
            @Parameter(description = STATUS, example = "AKTIF")
            @RequestParam(defaultValue = "AKTIF") String status) {
        return Map.of("status", status, "total", service.totalByStatus(status));
    }

    @GetMapping
    @Operation(
            summary = "Statistik lengkap karyawan pada satu status",
            description = "Memanggil stored procedure dengan beberapa OUT parameter sekaligus.")
    @ApiResponse(responseCode = "200", description = "Statistik karyawan")
    public StatistikKaryawan statistik(
            @Parameter(description = STATUS, example = "AKTIF")
            @RequestParam(defaultValue = "AKTIF") String status) {
        return service.statistikByStatus(status);
    }
}
