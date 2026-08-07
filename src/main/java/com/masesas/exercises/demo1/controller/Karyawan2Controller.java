package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.Karyawan2Request;
import com.masesas.exercises.demo1.dto.Karyawan2Response;
import com.masesas.exercises.demo1.service.Karyawan2Service;
import org.springframework.data.domain.Page;
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
public class Karyawan2Controller {

    private final Karyawan2Service karyawan2Service;

    public Karyawan2Controller(Karyawan2Service karyawan2Service) {
        this.karyawan2Service = karyawan2Service;
    }

    @GetMapping
    public List<Karyawan2Response> getAll() {
        return karyawan2Service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Karyawan2Response> getById(@PathVariable Integer id) {
        Karyawan2Response karyawan = karyawan2Service.getById(id);
        if (karyawan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(karyawan);
    }

    @GetMapping("/status")
    public List<Karyawan2Response> getAllByStatus(@RequestParam String status) {
        return karyawan2Service.getAllByStatus(status);
    }

    @GetMapping("/page")
    public Page<Karyawan2Response> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return karyawan2Service.getPage(page, size);
    }

    @GetMapping("/search")
    public Page<Karyawan2Response> getPageByNama(
            @RequestParam String nama,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return karyawan2Service.getPageByNama(nama, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Karyawan2Response insert(@RequestBody Karyawan2Request request) {
        return karyawan2Service.insert(request);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public int insertBatch(@RequestBody List<Karyawan2Request> requests) {
        return karyawan2Service.insertBatch(requests);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Karyawan2Response> update(@PathVariable Integer id, @RequestBody Karyawan2Request request) {
        Karyawan2Response karyawan = karyawan2Service.update(id, request);
        if (karyawan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(karyawan);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!karyawan2Service.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
