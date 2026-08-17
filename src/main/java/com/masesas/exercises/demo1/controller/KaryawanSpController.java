package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.model.HasilProsesKaryawan;
import com.masesas.exercises.demo1.model.ProsesKaryawanRequest;
import com.masesas.exercises.demo1.service.KaryawanSpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping("/api/sp/karyawan")
@RequiredArgsConstructor
@Tag(name = "KaryawanSp", description = "Pemanggilan stored procedure sp_proses_karyawan, "
        + "disediakan dalam dua jalur — JdbcTemplate dan JPA — supaya hasil keduanya bisa "
        + "dibandingkan. Butuh token, tanpa pembatasan peran.")
@SecurityRequirement(name = "karyawanAuth")
@SecurityRequirement(name = "customerAuth")
public class KaryawanSpController {

    private static final String ID_KARYAWAN = "ID karyawan yang diproses";
    private static final String MODE = "Mode pemrosesan yang diteruskan ke stored procedure";
    private static final String PESAN_400 =
            "Stored procedure menolak permintaan lewat RAISE EXCEPTION";

    private final KaryawanSpService service;

    @GetMapping("/{id}")
    @Operation(summary = "Baca hasil proses karyawan lewat JdbcTemplate")
    @ApiResponse(responseCode = "200", description = "Hasil dari stored procedure")
    @ApiResponse(responseCode = "400", description = PESAN_400, content = @Content)
    public HasilProsesKaryawan lihatViaJdbc(
            @Parameter(description = ID_KARYAWAN, example = "7")
            @PathVariable Integer id,
            @Parameter(description = MODE, example = "LENGKAP")
            @RequestParam(required = false) String mode) {
        return service.lihatViaJdbc(id, mode);
    }

    @GetMapping("/{id}/jpa")
    @Operation(summary = "Baca hasil proses karyawan lewat JPA")
    @ApiResponse(responseCode = "200", description = "Hasil dari stored procedure")
    @ApiResponse(responseCode = "400", description = PESAN_400, content = @Content)
    public HasilProsesKaryawan lihatViaJpa(
            @Parameter(description = ID_KARYAWAN, example = "7")
            @PathVariable Integer id,
            @Parameter(description = MODE, example = "LENGKAP")
            @RequestParam(required = false) String mode) {
        return service.lihatViaJpa(id, mode);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Ubah sekaligus baca data karyawan lewat JdbcTemplate",
            description = "Stored procedure melakukan update dan mengembalikan hasilnya "
                    + "dalam satu panggilan.")
    @ApiResponse(responseCode = "200", description = "Hasil setelah pemrosesan")
    @ApiResponse(responseCode = "400", description = PESAN_400, content = @Content)
    public HasilProsesKaryawan prosesViaJdbc(
            @Parameter(description = ID_KARYAWAN, example = "7")
            @PathVariable Integer id,
            @RequestBody ProsesKaryawanRequest request) {
        return service.prosesViaJdbc(id, request);
    }

    @PutMapping("/{id}/jpa")
    @Operation(summary = "Ubah sekaligus baca data karyawan lewat JPA")
    @ApiResponse(responseCode = "200", description = "Hasil setelah pemrosesan")
    @ApiResponse(responseCode = "400", description = PESAN_400, content = @Content)
    public HasilProsesKaryawan prosesViaJpa(
            @Parameter(description = ID_KARYAWAN, example = "7")
            @PathVariable Integer id,
            @RequestBody ProsesKaryawanRequest request) {
        return service.prosesViaJpa(id, request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleSpError(DataAccessException ex) {
        Throwable akar = ex.getMostSpecificCause();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", akar.getMessage()));
    }
}
