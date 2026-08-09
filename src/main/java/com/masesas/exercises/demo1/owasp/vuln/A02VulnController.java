package com.masesas.exercises.demo1.owasp.vuln;

import com.masesas.exercises.demo1.dto.DetailKaryawanResponse;
import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.KaryawanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/api/vuln")
@Profile("owasp-demo")
@RequiredArgsConstructor
public class A02VulnController {

    private final KaryawanRepository karyawanRepository;

    @GetMapping("/karyawan/{id}/detail")
    public DetailKaryawanResponse detail(@PathVariable Integer id) {
        Karyawan karyawan = karyawanRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Karyawan", id));
        return DetailKaryawanResponse.fromLengkap(karyawan.getDetailKaryawan());
    }

    @GetMapping("/hash")
    public Map<String, String> hash(@RequestParam String password) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest(password.getBytes(StandardCharsets.UTF_8));
            return Map.of("algoritma", "MD5", "hash", HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
