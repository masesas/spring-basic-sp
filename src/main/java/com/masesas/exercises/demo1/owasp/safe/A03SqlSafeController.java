package com.masesas.exercises.demo1.owasp.safe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/safe/karyawan")
@Validated
@RequiredArgsConstructor
public class A03SqlSafeController {

    private static final String BASE_SQL =
            "SELECT id, nama, alamat, status FROM masesas.karyawan WHERE deleted_date IS NULL";

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/search")
    public List<Map<String, Object>> search(
            @RequestParam
            @NotBlank(message = "nama wajib diisi")
            @Size(max = 100, message = "nama maksimal 100 karakter")
            String nama) {
        return jdbcTemplate.queryForList(BASE_SQL + " AND nama = ?", nama);
    }

    @GetMapping("/sort")
    public List<Map<String, Object>> sort(@RequestParam String by) {
        return jdbcTemplate.queryForList(BASE_SQL + " ORDER BY " + SortField.dari(by).kolom());
    }
}
