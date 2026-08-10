package com.masesas.exercises.demo1.owasp.vuln;

import com.masesas.exercises.demo1.dto.FotoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/vuln/karyawan")
@Profile("owasp-demo")
@RequiredArgsConstructor
public class A10VulnController {

    private static final int PANJANG_CUPLIKAN = 300;

    private final RestTemplate restTemplate;

    @PostMapping("/{id}/foto")
    public Map<String, Object> ambilFoto(@PathVariable Integer id, @RequestBody FotoRequest request) {
        ResponseEntity<String> balasan = restTemplate.getForEntity(request.getUrl(), String.class);
        String isi = balasan.getBody() == null ? "" : balasan.getBody();

        return Map.of(
                "idKaryawan", id,
                "url", request.getUrl(),
                "status", balasan.getStatusCode().value(),
                "isi", isi.length() > PANJANG_CUPLIKAN ? isi.substring(0, PANJANG_CUPLIKAN) : isi);
    }
}
