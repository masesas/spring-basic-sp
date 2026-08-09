package com.masesas.exercises.demo1.owasp.safe;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mengenkripsi baris {@code detail_karyawan} yang masih berupa teks biasa.
 *
 * <p>Dijalankan sekali dengan profil {@code migrasi-enkripsi}:
 * {@code mvn spring-boot:run -Dspring-boot.run.profiles=migrasi-enkripsi}
 *
 * <p>Membaca lewat JdbcTemplate, bukan lewat entity, supaya nilai mentahnya terlihat
 * tanpa melalui converter. Aman dijalankan berulang: baris yang sudah bertanda
 * {@code enc:v1:} dilewati.
 */
@Component
@Profile("migrasi-enkripsi")
@RequiredArgsConstructor
@Slf4j
public class EnkripsiDetailRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final CryptoConverter cryptoConverter;

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> baris = jdbcTemplate.queryForList(
                "SELECT id, nik, npwp FROM masesas.detail_karyawan");

        int diproses = 0;
        for (Map<String, Object> kolom : baris) {
            String nik = (String) kolom.get("nik");
            String npwp = (String) kolom.get("npwp");

            if (CryptoConverter.sudahTerenkripsi(nik) && CryptoConverter.sudahTerenkripsi(npwp)) {
                continue;
            }

            jdbcTemplate.update(
                    "UPDATE masesas.detail_karyawan SET nik = ?, npwp = ? WHERE id = ?",
                    enkripsiJikaPerlu(nik),
                    enkripsiJikaPerlu(npwp),
                    kolom.get("id"));
            diproses++;
        }

        log.info("Migrasi enkripsi selesai: {} dari {} baris dienkripsi", diproses, baris.size());
    }

    private String enkripsiJikaPerlu(String nilai) {
        return CryptoConverter.sudahTerenkripsi(nilai)
                ? nilai
                : cryptoConverter.convertToDatabaseColumn(nilai);
    }
}
