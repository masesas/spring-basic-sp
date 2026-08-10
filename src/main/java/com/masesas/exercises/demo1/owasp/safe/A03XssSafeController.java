package com.masesas.exercises.demo1.owasp.safe;

import com.masesas.exercises.demo1.dto.KaryawanTeksRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/safe/karyawan")
@RequiredArgsConstructor
@Slf4j
public class A03XssSafeController {

    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/teks")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> simpan(@RequestBody KaryawanTeksRequest request) {
        String nama = InputSanitizer.requireBebasMarkup(request.getNama(), "nama");
        String alamat = InputSanitizer.requireBebasMarkup(request.getAlamat(), "alamat");

        Integer id = jdbcTemplate.queryForObject(
                "INSERT INTO masesas.karyawan (nama, alamat, dob, status, created_date) "
                        + "VALUES (?, ?, DATE '1990-01-01', 'AKTIF', now()) RETURNING id",
                Integer.class,
                nama,
                alamat);

        return jdbcTemplate.queryForMap(
                "SELECT id, nama, alamat FROM masesas.karyawan WHERE id = ?", id);
    }

    @GetMapping("/log")
    public Map<String, String> catatPencarian(@RequestParam String keyword) {
        log.info("Pencarian karyawan dengan keyword={}", InputSanitizer.untukLog(keyword));
        return Map.of("keyword", keyword);
    }
}
