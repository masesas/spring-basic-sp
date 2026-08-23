package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.BaseApiResponse;
import com.masesas.exercises.demo1.dto.Karyawan2Request;
import com.masesas.exercises.demo1.dto.Karyawan2Response;
import com.masesas.exercises.demo1.service.Karyawan2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/karyawan2")
@Tag(name = "Karyawan2", description = "Data karyawan lewat JdbcTemplate, bukan JPA. "
        + "Butuh token, tanpa pembatasan peran.")
@SecurityRequirement(name = "karyawanAuth")
@SecurityRequirement(name = "customerAuth")
public class Karyawan2Controller {

    private static final String PESAN_TIDAK_ADA = "Karyawan tidak ditemukan";
    private static final String KODE_TIDAK_ADA = "NOT_FOUND";

    private final Karyawan2Service karyawan2Service;

    public Karyawan2Controller(Karyawan2Service karyawan2Service) {
        this.karyawan2Service = karyawan2Service;
    }

    @GetMapping
    @Operation(summary = "Seluruh karyawan tanpa paging")
    @ApiResponse(responseCode = "200", description = "Seluruh karyawan")
    public BaseApiResponse<List<Karyawan2Response>> getAll() {
        return BaseApiResponse.ok("Seluruh karyawan", karyawan2Service.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ambil satu karyawan berdasarkan id")
    @ApiResponse(responseCode = "200", description = "Karyawan ditemukan")
    @ApiResponse(responseCode = "404", description = "Karyawan tidak ada", content = @Content)
    public ResponseEntity<BaseApiResponse<Karyawan2Response>> getById(
            @Parameter(description = "ID karyawan", example = "1")
            @PathVariable Integer id) {
        Karyawan2Response karyawan = karyawan2Service.getById(id);
        if (karyawan == null) {
            return tidakDitemukan();
        }
        return ResponseEntity.ok(BaseApiResponse.ok("Karyawan ditemukan", karyawan));
    }

    @GetMapping("/status")
    @Operation(summary = "Daftar karyawan berdasarkan status kepegawaian")
    @ApiResponse(responseCode = "200", description = "Karyawan dengan status tersebut")
    public BaseApiResponse<List<Karyawan2Response>> getAllByStatus(
            @Parameter(description = "Status kepegawaian", example = "AKTIF")
            @RequestParam String status) {
        return BaseApiResponse.ok(
                "Karyawan dengan status tersebut", karyawan2Service.getAllByStatus(status));
    }

    @GetMapping("/page")
    @Operation(summary = "Daftar karyawan dengan paging manual")
    @ApiResponse(responseCode = "200", description = "Satu halaman karyawan")
    public BaseApiResponse<List<Karyawan2Response>> getPage(
            @Parameter(description = "Nomor halaman, dimulai dari 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Jumlah baris per halaman", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return BaseApiResponse.page("Satu halaman karyawan", karyawan2Service.getPage(page, size));
    }

    @GetMapping("/search")
    @Operation(summary = "Cari karyawan berdasarkan nama")
    @ApiResponse(responseCode = "200", description = "Satu halaman hasil pencarian")
    public BaseApiResponse<List<Karyawan2Response>> getPageByNama(
            @Parameter(description = "Potongan nama yang dicari", example = "budi")
            @RequestParam String nama,
            @Parameter(description = "Nomor halaman, dimulai dari 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Jumlah baris per halaman", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return BaseApiResponse.page(
                "Satu halaman hasil pencarian", karyawan2Service.getPageByNama(nama, page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tambah satu karyawan")
    @ApiResponse(responseCode = "201", description = "Karyawan dibuat")
    public BaseApiResponse<Karyawan2Response> insert(@RequestBody Karyawan2Request request) {
        return BaseApiResponse.created("Karyawan dibuat", karyawan2Service.insert(request));
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Tambah banyak karyawan sekaligus",
            description = "Dijalankan sebagai satu batch JDBC. Balasannya jumlah baris yang masuk.")
    @ApiResponse(responseCode = "201", description = "Jumlah baris yang berhasil dimasukkan")
    public BaseApiResponse<Integer> insertBatch(@RequestBody List<Karyawan2Request> requests) {
        return BaseApiResponse.created(
                "Jumlah baris yang berhasil dimasukkan", karyawan2Service.insertBatch(requests));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ubah data karyawan")
    @ApiResponse(responseCode = "200", description = "Karyawan diperbarui")
    @ApiResponse(responseCode = "404", description = "Karyawan tidak ada", content = @Content)
    public ResponseEntity<BaseApiResponse<Karyawan2Response>> update(
            @Parameter(description = "ID karyawan", example = "1")
            @PathVariable Integer id,
            @RequestBody Karyawan2Request request) {
        Karyawan2Response karyawan = karyawan2Service.update(id, request);
        if (karyawan == null) {
            return tidakDitemukan();
        }
        return ResponseEntity.ok(BaseApiResponse.ok("Karyawan diperbarui", karyawan));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus karyawan")
    @ApiResponse(responseCode = "204", description = "Karyawan dihapus")
    @ApiResponse(responseCode = "404", description = "Karyawan tidak ada", content = @Content)
    public ResponseEntity<BaseApiResponse<Karyawan2Response>> delete(
            @Parameter(description = "ID karyawan", example = "1")
            @PathVariable Integer id) {
        if (!karyawan2Service.delete(id)) {
            return tidakDitemukan();
        }
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<BaseApiResponse<Karyawan2Response>> tidakDitemukan() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                BaseApiResponse.error(HttpStatus.NOT_FOUND, PESAN_TIDAK_ADA, KODE_TIDAK_ADA));
    }
}
