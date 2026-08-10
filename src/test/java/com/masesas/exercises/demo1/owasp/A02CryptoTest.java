package com.masesas.exercises.demo1.owasp;

import com.masesas.exercises.demo1.owasp.safe.CryptoConverter;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("owasp-demo")
@Transactional
class A02CryptoTest {

    private static final String NIK = "3273010101900001";
    private static final String NPWP = "092542943407000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CryptoConverter cryptoConverter;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    private Integer idKaryawan;

    @BeforeEach
    void tanamData() {
        Integer idDetail = jdbcTemplate.queryForObject(
                "INSERT INTO masesas.detail_karyawan (nik, npwp, created_date) "
                        + "VALUES (?, ?, now()) RETURNING id",
                Integer.class,
                cryptoConverter.convertToDatabaseColumn(NIK),
                cryptoConverter.convertToDatabaseColumn(NPWP));

        idKaryawan = jdbcTemplate.queryForObject(
                "INSERT INTO masesas.karyawan (nama, alamat, dob, status, detail_karyawan, created_date) "
                        + "VALUES ('Uji A02', 'Jakarta', ?, 'AKTIF', ?, now()) RETURNING id",
                Integer.class,
                Date.valueOf("1990-01-01"),
                idDetail);
    }

    @Test
    @DisplayName("AMAN: NIK di database tersimpan terenkripsi, bukan teks biasa")
    void safe_nikTersimpanTerenkripsi() {
        String tersimpan = jdbcTemplate.queryForObject(
                "SELECT nik FROM masesas.detail_karyawan d "
                        + "JOIN masesas.karyawan k ON k.detail_karyawan = d.id WHERE k.id = ?",
                String.class, idKaryawan);

        assertThat(tersimpan).isNotEqualTo(NIK);
        assertThat(CryptoConverter.sudahTerenkripsi(tersimpan)).isTrue();
    }

    @Test
    @DisplayName("AMAN: enkripsi bolak-balik menghasilkan nilai asli")
    void safe_enkripsiBolakBalik() {
        String terenkripsi = cryptoConverter.convertToDatabaseColumn(NIK);

        assertThat(cryptoConverter.convertToEntityAttribute(terenkripsi)).isEqualTo(NIK);
    }

    @Test
    @DisplayName("AMAN: nilai sama dienkripsi dua kali menghasilkan ciphertext berbeda")
    void safe_ivAcakMencegahPolaBerulang() {
        String pertama = cryptoConverter.convertToDatabaseColumn(NIK);
        String kedua = cryptoConverter.convertToDatabaseColumn(NIK);

        assertThat(pertama).isNotEqualTo(kedua);
    }

    @Test
    @DisplayName("AMAN: teks biasa lama tetap terbaca selama masa transisi")
    void safe_teksBiasaLamaTetapTerbaca() {
        assertThat(cryptoConverter.convertToEntityAttribute(NIK)).isEqualTo(NIK);
    }

    @Test
    @DisplayName("RENTAN: endpoint demo mengembalikan NIK dan NPWP utuh")
    void vuln_nikDikembalikanUtuh() throws Exception {
        mockMvc.perform(get("/api/vuln/karyawan/" + idKaryawan + "/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nik").value(NIK))
                .andExpect(jsonPath("$.npwp").value(NPWP));
    }

    @Test
    @DisplayName("AMAN: response hanya memuat 4 digit terakhir")
    void safe_nikTersamarDiResponse() throws Exception {
        mockMvc.perform(get("/api/safe/karyawan/" + idKaryawan + "/detail")
                        .header(HttpHeaders.AUTHORIZATION, bearer("hr@masesas.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nik").value("************0001"))
                .andExpect(jsonPath("$.npwp").value("***********7000"));
    }

    @Test
    @DisplayName("RENTAN: hash MD5 selalu sama untuk password yang sama")
    void vuln_md5DapatDitebak() throws Exception {
        mockMvc.perform(get("/api/vuln/hash").param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hash").value("482c811da5d5b4bc6d497ffa98491e38"));
    }

    @Test
    @DisplayName("AMAN: bcrypt bergaram, hash berbeda tiap kali tapi tetap cocok")
    void safe_bcryptBergaram() {
        String pertama = passwordEncoder.encode("password123");
        String kedua = passwordEncoder.encode("password123");

        assertThat(pertama).isNotEqualTo(kedua);
        assertThat(pertama).startsWith("{bcrypt}$2a$12$");
        assertThat(passwordEncoder.matches("password123", pertama)).isTrue();
    }

    private String bearer(String username) {
        AppUser user = userDetailsService.loadUserByUsername(username);
        return "Bearer " + jwtService.issue(user, Instant.now());
    }
}
