package com.masesas.exercises.demo1.owasp.safe;

import com.masesas.exercises.demo1.dto.FotoRequest;
import com.masesas.exercises.demo1.exception.InvalidRequestException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/safe/karyawan")
public class A10SafeController {

    private static final long UKURAN_MAKS = 2L * 1024 * 1024;

    private final RestTemplate restTemplate;

    public A10SafeController(@Qualifier("restTemplateAman") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/{id}/foto")
    public Map<String, Object> ambilFoto(@PathVariable Integer id, @RequestBody FotoRequest request) {
        URI uri = UrlGuard.periksa(request.getUrl());

        ResponseEntity<byte[]> balasan = restTemplate.getForEntity(uri, byte[].class);
        requireBukanRedirect(balasan);

        byte[] isi = balasan.getBody() == null ? new byte[0] : balasan.getBody();
        requireUkuranWajar(isi.length);
        requireGambar(balasan.getHeaders().getContentType());

        return Map.of(
                "idKaryawan", id,
                "url", uri.toString(),
                "ukuran", isi.length,
                "tipe", String.valueOf(balasan.getHeaders().getContentType()));
    }

    private void requireBukanRedirect(ResponseEntity<byte[]> balasan) {
        if (balasan.getStatusCode().is3xxRedirection()) {
            throw new InvalidRequestException(
                    "alamat mengarahkan ulang ke tempat lain, tidak diikuti");
        }
    }

    private void requireUkuranWajar(int ukuran) {
        if (ukuran > UKURAN_MAKS) {
            throw new InvalidRequestException("berkas melebihi batas 2 MB");
        }
    }

    private void requireGambar(MediaType tipe) {
        if (tipe == null || !"image".equalsIgnoreCase(tipe.getType())) {
            throw new InvalidRequestException("isi alamat bukan gambar, melainkan " + tipe);
        }
    }
}
