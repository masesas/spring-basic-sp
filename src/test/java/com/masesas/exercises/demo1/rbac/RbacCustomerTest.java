package com.masesas.exercises.demo1.rbac;

import com.masesas.exercises.demo1.repository.CustomerRepository;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RbacCustomerTest {

    private static final String CUSTOMER_SEED = "customer1@masesas.test";
    private static final String CUSTOMER_BARU = "customer.baru@masesas.test";
    private static final String EMAIL_KARYAWAN = "admin@masesas.test";
    private static final String EMAIL_HR = "hr@masesas.test";
    private static final String PASSWORD_BARU = "RahasiaKuat123";
    private static final String DAFTAR_CUSTOMER = "/api/customer";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Autowired
    private CustomerRepository customerRepository;

    @Value("${app.security.password}")
    private String demoPassword;

    @BeforeEach
    @AfterEach
    void hapusCustomerUjiCoba() {
        customerRepository.findByEmailAndDeletedDateIsNull(CUSTOMER_BARU)
                .ifPresent(customerRepository::delete);
    }

    @Test
    @DisplayName("register customer baru dibalas 201 dan responsnya tidak memuat password")
    void registerBerhasil() throws Exception {
        mockMvc.perform(register(CUSTOMER_BARU))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(CUSTOMER_BARU))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("register dengan email yang sudah dipakai customer lain dibalas 409")
    void registerEmailCustomerGandaDitolak() throws Exception {
        mockMvc.perform(register(CUSTOMER_BARU)).andExpect(status().isCreated());

        mockMvc.perform(register(CUSTOMER_BARU)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("register dengan email milik karyawan dibalas 409")
    void registerEmailKaryawanDitolak() throws Exception {
        mockMvc.perform(register(EMAIL_KARYAWAN)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("customer yang baru register langsung bisa login")
    void registerLaluLogin() throws Exception {
        mockMvc.perform(register(CUSTOMER_BARU)).andExpect(status().isCreated());

        mockMvc.perform(loginCustomer(CUSTOMER_BARU, PASSWORD_BARU))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tipe").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.roles[0]").value("CUSTOMER"));
    }

    @Test
    @DisplayName("customer membaca profilnya sendiri lewat /api/customer/me")
    void customerMembacaProfilSendiri() throws Exception {
        mockMvc.perform(get("/api/customer/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerCustomer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(CUSTOMER_SEED));
    }

    @Test
    @DisplayName("token customer ditolak di endpoint karyawan")
    void customerDitolakDiEndpointKaryawan() throws Exception {
        mockMvc.perform(get("/api/karyawan/all")
                        .header(HttpHeaders.AUTHORIZATION, bearerCustomer()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("token karyawan ditolak di endpoint customer")
    void karyawanDitolakDiEndpointCustomer() throws Exception {
        AppUser karyawan = userDetailsService.findKaryawan(EMAIL_KARYAWAN).orElseThrow();

        mockMvc.perform(get("/api/customer/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issue(karyawan, Instant.now())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("karyawan pemegang CUSTOMER_READ membaca satu halaman daftar customer")
    void karyawanMembacaDaftarCustomer() throws Exception {
        mockMvc.perform(get(DAFTAR_CUSTOMER)
                        .param("size", "5")
                        .header(HttpHeaders.AUTHORIZATION, bearerKaryawan(EMAIL_KARYAWAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].password").doesNotExist())
                .andExpect(jsonPath("$.meta.size").value(5));
    }

    @Test
    @DisplayName("HR tidak memegang CUSTOMER_READ sehingga daftar customer ditolak 403")
    void hrDitolakDiDaftarCustomer() throws Exception {
        mockMvc.perform(get(DAFTAR_CUSTOMER)
                        .header(HttpHeaders.AUTHORIZATION, bearerKaryawan(EMAIL_HR)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("customer tidak bisa membaca daftar seluruh customer")
    void customerDitolakDiDaftarCustomer() throws Exception {
        mockMvc.perform(get(DAFTAR_CUSTOMER)
                        .header(HttpHeaders.AUTHORIZATION, bearerCustomer()))
                .andExpect(status().isForbidden());
    }

    private RequestBuilder register(String email) {
        return post("/api/auth/customer/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nama\":\"Customer Baru\",\"email\":\"" + email
                        + "\",\"password\":\"" + PASSWORD_BARU + "\"}");
    }

    private RequestBuilder loginCustomer(String email, String password) {
        return post("/api/auth/customer/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    private String bearerCustomer() {
        AppUser customer = userDetailsService.findCustomer(CUSTOMER_SEED).orElseThrow();
        return "Bearer " + jwtService.issue(customer, Instant.now());
    }

    private String bearerKaryawan(String email) {
        AppUser karyawan = userDetailsService.findKaryawan(email).orElseThrow();
        return "Bearer " + jwtService.issue(karyawan, Instant.now());
    }
}
