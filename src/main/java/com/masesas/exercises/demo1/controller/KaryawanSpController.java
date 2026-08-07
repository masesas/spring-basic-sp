package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.model.HasilProsesKaryawan;
import com.masesas.exercises.demo1.model.ProsesKaryawanRequest;
import com.masesas.exercises.demo1.service.KaryawanSpService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Endpoint untuk mencoba sp_proses_karyawan lewat JdbcTemplate maupun JPA. */
@RestController
@RequestMapping("/api/sp/karyawan")
@RequiredArgsConstructor
public class KaryawanSpController {

    private final KaryawanSpService service;

    /** GET /api/sp/karyawan/7?mode=LENGKAP - baca data lewat JdbcTemplate. */
    @GetMapping("/{id}")
    public HasilProsesKaryawan lihatViaJdbc(@PathVariable Integer id,
                                            @RequestParam(required = false) String mode) {
        return service.lihatViaJdbc(id, mode);
    }

    /** GET /api/sp/karyawan/7/jpa?mode=LENGKAP - baca data lewat JPA. */
    @GetMapping("/{id}/jpa")
    public HasilProsesKaryawan lihatViaJpa(@PathVariable Integer id,
                                           @RequestParam(required = false) String mode) {
        return service.lihatViaJpa(id, mode);
    }

    /** PUT /api/sp/karyawan/7 - update sekaligus baca lewat JdbcTemplate. */
    @PutMapping("/{id}")
    public HasilProsesKaryawan prosesViaJdbc(@PathVariable Integer id,
                                             @RequestBody ProsesKaryawanRequest request) {
        return service.prosesViaJdbc(id, request);
    }

    /** PUT /api/sp/karyawan/7/jpa - update sekaligus baca lewat JPA. */
    @PutMapping("/{id}/jpa")
    public HasilProsesKaryawan prosesViaJpa(@PathVariable Integer id,
                                            @RequestBody ProsesKaryawanRequest request) {
        return service.prosesViaJpa(id, request);
    }

    /** RAISE EXCEPTION dari stored procedure diterjemahkan menjadi HTTP 400. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleSpError(DataAccessException ex) {
        Throwable akar = ex.getMostSpecificCause();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", akar.getMessage()));
    }
}
