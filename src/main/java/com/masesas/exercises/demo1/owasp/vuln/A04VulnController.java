package com.masesas.exercises.demo1.owasp.vuln;

import com.masesas.exercises.demo1.dto.PayrollUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/vuln")
@Profile("owasp-demo")
@RequiredArgsConstructor
public class A04VulnController {

    private final JdbcTemplate jdbcTemplate;

    @PutMapping("/payroll/{idKaryawan}/{periode}")
    public Map<String, Object> revisi(
            @PathVariable Integer idKaryawan,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periode,
            @RequestBody PayrollUpdateRequest request) {

        jdbcTemplate.update(
                "UPDATE masesas.payroll_karyawan SET gaji_pokok = ?, updated_date = now() "
                        + "WHERE id_karyawan = ? AND periode = ?",
                request.gajiPokok(),
                idKaryawan,
                java.sql.Date.valueOf(periode.withDayOfMonth(1)));

        return jdbcTemplate.queryForMap(
                "SELECT id_karyawan, periode, gaji_pokok, status FROM masesas.payroll_karyawan "
                        + "WHERE id_karyawan = ? AND periode = ?",
                idKaryawan,
                java.sql.Date.valueOf(periode.withDayOfMonth(1)));
    }
}
