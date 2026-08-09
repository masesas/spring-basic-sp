package com.masesas.exercises.demo1.owasp;

import com.masesas.exercises.demo1.owasp.safe.UrlGuard;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Menjalankan server HTTP kecil di 127.0.0.1 sebagai pengganti layanan internal yang
 * seharusnya tidak bisa dijangkau dari luar — peran yang di dunia nyata dipegang Redis,
 * database, atau endpoint metadata cloud.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("owasp-demo")
class A10SsrfTest {

    private static final String RAHASIA = "RAHASIA-INTERNAL-TOKEN-12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    private HttpServer layananInternal;
    private String alamatInternal;

    @BeforeEach
    void hidupkanLayananInternal() throws IOException {
        layananInternal = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        layananInternal.createContext("/rahasia", exchange -> {
            byte[] isi = RAHASIA.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, isi.length);
            try (OutputStream keluaran = exchange.getResponseBody()) {
                keluaran.write(isi);
            }
        });
        layananInternal.start();
        alamatInternal = "http://127.0.0.1:" + layananInternal.getAddress().getPort() + "/rahasia";
    }

    @AfterEach
    void matikanLayananInternal() {
        layananInternal.stop(0);
    }

    @Test
    @DisplayName("RENTAN: server dipaksa menembak layanan internal dan isinya ikut terbawa keluar")
    void vuln_layananInternalTerjangkau() throws Exception {
        mockMvc.perform(post("/api/vuln/karyawan/1/foto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + alamatInternal + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isi").value(RAHASIA));
    }

    @Test
    @DisplayName("AMAN: alamat loopback ditolak 400 tanpa satu pun panggilan jaringan")
    void safe_loopbackDitolak() throws Exception {
        mockMvc.perform(post("/api/safe/karyawan/1/foto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + alamatInternal + "\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("AMAN: endpoint metadata cloud 169.254.169.254 ditolak")
    void safe_metadataCloudDitolak() throws Exception {
        mockMvc.perform(post("/api/safe/karyawan/1/foto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://169.254.169.254/latest/meta-data/\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("alamat internal")));
    }

    @Test
    @DisplayName("AMAN: skema selain https ditolak")
    void safe_skemaHttpDitolak() throws Exception {
        mockMvc.perform(post("/api/safe/karyawan/1/foto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"http://contoh.example/foto.png\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("hanya skema https")));
    }

    @Test
    @DisplayName("UrlGuard menolak seluruh rentang alamat internal")
    void urlGuard_menolakSeluruhRentangInternal() {
        String[] alamatInternal = {
                "https://127.0.0.1/a",
                "https://localhost/a",
                "https://169.254.169.254/a",
                "https://10.0.0.1/a",
                "https://192.168.1.1/a",
                "https://172.16.0.1/a",
                "https://0.0.0.0/a",
                "https://[::1]/a"
        };

        for (String url : alamatInternal) {
            assertThatThrownBy(() -> UrlGuard.periksa(url))
                    .as("seharusnya ditolak: %s", url)
                    .hasMessageContaining("alamat internal");
        }
    }

    @Test
    @DisplayName("UrlGuard menolak bentuk url yang tidak sah")
    void urlGuard_menolakBentukTidakSah() {
        assertThatThrownBy(() -> UrlGuard.periksa("")).hasMessageContaining("wajib diisi");
        assertThatThrownBy(() -> UrlGuard.periksa("bukan-url")).hasMessageContaining("host");
        assertThatThrownBy(() -> UrlGuard.periksa("file:///etc/passwd")).hasMessageContaining("host");
        assertThatThrownBy(() -> UrlGuard.periksa("https://host-yang-tidak-ada.invalid/a"))
                .hasMessageContaining("host tidak dikenal");
    }

    private String bearer() {
        AppUser user = userDetailsService.loadUserByUsername("hr");
        return "Bearer " + jwtService.issue(user, Instant.now());
    }
}
