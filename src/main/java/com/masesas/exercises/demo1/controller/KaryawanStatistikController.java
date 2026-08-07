package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.model.StatistikKaryawan;
import com.masesas.exercises.demo1.service.KaryawanStatistikService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Endpoint untuk stored procedure ber-OUT parameter. */
@RestController
@RequestMapping("/api/sp/karyawan/statistik")
@RequiredArgsConstructor
public class KaryawanStatistikController {

    private final KaryawanStatistikService service;

    /** GET /api/sp/karyawan/statistik/total?status=AKTIF - satu OUT parameter. */
    @GetMapping("/total")
    public Map<String, Object> total(@RequestParam(defaultValue = "AKTIF") String status) {
        return Map.of("status", status, "total", service.totalByStatus(status));
    }

    /** GET /api/sp/karyawan/statistik?status=AKTIF - beberapa OUT parameter. */
    @GetMapping
    public StatistikKaryawan statistik(@RequestParam(defaultValue = "AKTIF") String status) {
        return service.statistikByStatus(status);
    }
}
