package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.CreateKaryawanRequest;
import com.masesas.exercises.demo1.dto.DetailKaryawanRequest;
import com.masesas.exercises.demo1.dto.KaryawanResponse;
import com.masesas.exercises.demo1.dto.UpdateKaryawanRequest;
import com.masesas.exercises.demo1.service.KaryawanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/karyawan")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','MARKETING','SALES','HR','KARYAWAN')")
@Tag(name = "Karyawan", description = "Data induk karyawan lewat JPA. "
        + "Semua endpoint butuh peran karyawan; membuat, mengubah, dan menghapus dibatasi lebih ketat lagi.")
@SecurityRequirement(name = "karyawanAuth")
public class KaryawanController {

    private final KaryawanService karyawanService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(
            summary = "Buat karyawan baru",
            description = "Butuh peran ADMIN atau MANAGER. Email harus belum terpakai.")
    @ApiResponse(responseCode = "201", description = "Karyawan dibuat")
    public KaryawanResponse create(@Valid @RequestBody CreateKaryawanRequest request) {
        return karyawanService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ambil satu karyawan berdasarkan id")
    @ApiResponse(responseCode = "200", description = "Karyawan ditemukan")
    public KaryawanResponse findById(
            @Parameter(description = "ID karyawan", example = "1")
            @PathVariable Integer id) {
        return karyawanService.findById(id);
    }

    @GetMapping
    @Operation(
            summary = "Daftar karyawan dengan paging standar Spring Data",
            description = "Memakai parameter page, size, dan sort — contoh: "
                    + "?page=0&size=10&sort=nama,asc. Ukuran halaman maksimal 100.")
    @ApiResponse(responseCode = "200", description = "Satu halaman karyawan")
    public Page<KaryawanResponse> findAll(Pageable pageable) {
        return karyawanService.findAll(pageable);
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Daftar karyawan dengan paging manual",
            description = "Butuh peran ADMIN. Nomor halaman dan ukurannya ditulis sebagai "
                    + "parameter biasa, bukan lewat Pageable.")
    @ApiResponse(responseCode = "200", description = "Satu halaman karyawan")
    public Page<KaryawanResponse> findPage(
            @Parameter(description = "Nomor halaman, dimulai dari 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Jumlah baris per halaman", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return karyawanService.findPage(page, size);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Cari karyawan berdasarkan nama",
            description = "Pencarian mengandung (contains), tidak membedakan huruf besar-kecil.")
    @ApiResponse(responseCode = "200", description = "Satu halaman hasil pencarian")
    public Page<KaryawanResponse> findPageByNama(
            @Parameter(description = "Potongan nama yang dicari", example = "budi")
            @RequestParam String nama,
            @Parameter(description = "Nomor halaman, dimulai dari 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Jumlah baris per halaman", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return karyawanService.findPageByNama(nama, page, size);
    }

    @GetMapping("/all")
    @Operation(
            summary = "Seluruh karyawan tanpa paging",
            description = "Hasilnya disimpan di cache Redis dan dibatalkan otomatis "
                    + "setiap kali ada perubahan data karyawan.")
    @ApiResponse(responseCode = "200", description = "Seluruh karyawan")
    public List<KaryawanResponse> findAllWithoutPaging() {
        return karyawanService.findAll();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(
            summary = "Ubah data karyawan",
            description = "Butuh peran ADMIN atau MANAGER.")
    @ApiResponse(responseCode = "200", description = "Karyawan diperbarui")
    public KaryawanResponse update(
            @Parameter(description = "ID karyawan", example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody UpdateKaryawanRequest request) {
        return karyawanService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Hapus karyawan",
            description = "Butuh peran ADMIN. Penghapusan bersifat soft delete — barisnya "
                    + "ditandai, bukan dibuang dari tabel.")
    @ApiResponse(responseCode = "204", description = "Karyawan dihapus")
    public void delete(
            @Parameter(description = "ID karyawan", example = "1")
            @PathVariable Integer id) {
        karyawanService.delete(id);
    }

    @PutMapping("/{id}/detail")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(
            summary = "Tambah atau ubah detail karyawan",
            description = "Butuh peran ADMIN atau MANAGER. Kolom nik dan npwp disimpan "
                    + "terenkripsi di database.")
    @ApiResponse(responseCode = "200", description = "Detail tersimpan")
    public KaryawanResponse upsertDetail(
            @Parameter(description = "ID karyawan", example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody DetailKaryawanRequest request) {
        return karyawanService.upsertDetail(id, request);
    }

    @DeleteMapping("/{id}/detail")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(
            summary = "Hapus detail karyawan saja",
            description = "Butuh peran ADMIN atau MANAGER. Data induk karyawannya tetap ada.")
    @ApiResponse(responseCode = "200", description = "Detail dihapus")
    public KaryawanResponse removeDetail(
            @Parameter(description = "ID karyawan", example = "1")
            @PathVariable Integer id) {
        return karyawanService.removeDetail(id);
    }

    @PostMapping(
            value = "/{id}/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Unggah foto avatar karyawan",
            description = "Berkas dikirim sebagai multipart dengan nama bagian file. "
                    + "Ukuran maksimal 2 MB.")
    @ApiResponse(responseCode = "200", description = "Avatar tersimpan")
    public KaryawanResponse uploadAvatar(
            @Parameter(description = "ID karyawan", example = "1")
            @PathVariable Integer id,
            @RequestPart("file") MultipartFile file
    ) {
        return karyawanService.uploadAvatar(id, file);
    }
}
