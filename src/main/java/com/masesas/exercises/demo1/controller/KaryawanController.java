package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.CreateKaryawanRequest;
import com.masesas.exercises.demo1.dto.DetailKaryawanRequest;
import com.masesas.exercises.demo1.dto.KaryawanResponse;
import com.masesas.exercises.demo1.dto.UpdateKaryawanRequest;
import com.masesas.exercises.demo1.service.KaryawanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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

/**
 * REST endpoint untuk data karyawan.
 * Controller hanya menerima request dan meneruskan ke service — tanpa logika bisnis.
 */
@RestController
@RequestMapping("/api/karyawan")
@RequiredArgsConstructor
public class KaryawanController {

    private final KaryawanService karyawanService;

    /** POST /api/karyawan — buat karyawan baru. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KaryawanResponse create(@RequestBody CreateKaryawanRequest request) {
        return karyawanService.create(request);
    }

    /** GET /api/karyawan/{id} — ambil satu karyawan. */
    @GetMapping("/{id}")
    public KaryawanResponse findById(@PathVariable Integer id) {
        return karyawanService.findById(id);
    }

    /** GET /api/karyawan?page=0&size=10&sort=nama,asc — ambil daftar karyawan. */
    @GetMapping
    public Page<KaryawanResponse> findAll(Pageable pageable) {
        return karyawanService.findAll(pageable);
    }

    @GetMapping("/page")
    public Page<KaryawanResponse> findPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return karyawanService.findPage(page, size);
    }

    @GetMapping("/search")
    public Page<KaryawanResponse> findPageByNama(
            @RequestParam String nama,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return karyawanService.findPageByNama(nama, page, size);
    }

    /** GET /api/karyawan/all — semua karyawan tanpa pagination (hasilnya di-cache di Redis). */
    @GetMapping("/all")
    public List<KaryawanResponse> findAllWithoutPaging() {
        return karyawanService.findAll();
    }

    /** PUT /api/karyawan/{id} — ubah data karyawan. */
    @PutMapping("/{id}")
    public KaryawanResponse update(@PathVariable Integer id, @RequestBody UpdateKaryawanRequest request) {
        return karyawanService.update(id, request);
    }

    /** DELETE /api/karyawan/{id} — soft delete karyawan. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        karyawanService.delete(id);
    }

    /** PUT /api/karyawan/{id}/detail — tambah atau ubah detail karyawan. */
    @PutMapping("/{id}/detail")
    public KaryawanResponse upsertDetail(@PathVariable Integer id, @RequestBody DetailKaryawanRequest request) {
        return karyawanService.upsertDetail(id, request);
    }

    /** DELETE /api/karyawan/{id}/detail — hapus detail karyawan saja. */
    @DeleteMapping("/{id}/detail")
    public KaryawanResponse removeDetail(@PathVariable Integer id) {
        return karyawanService.removeDetail(id);
    }
}
