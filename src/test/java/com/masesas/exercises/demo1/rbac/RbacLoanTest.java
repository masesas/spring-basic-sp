package com.masesas.exercises.demo1.rbac;

import com.masesas.exercises.demo1.dto.LoanApplicationRequest;
import com.masesas.exercises.demo1.dto.LoanApplicationResponse;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import com.masesas.exercises.demo1.service.LoanApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RbacLoanTest {

    private static final String ADMIN = "admin@masesas.test";
    private static final String MANAGER = "manager@masesas.test";
    private static final String MARKETING = "marketing@masesas.test";
    private static final String SALES = "sales@masesas.test";
    private static final String HR = "hr@masesas.test";
    private static final String CUSTOMER_SATU = "customer1@masesas.test";
    private static final String CUSTOMER_DUA = "customer2@masesas.test";

    private static final String PENGAJUAN = "/api/loan-application";
    private static final String PENGAJUAN_CUSTOMER = "/api/customer/loan-application";
    private static final BigDecimal SEPULUH_JUTA = new BigDecimal("10000000.00");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Autowired
    private LoanApplicationService loanApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer idProdukKta;

    @BeforeEach
    void ambilDataAcuan() {
        idProdukKta = jdbcTemplate.queryForObject(
                "select id from masesas.loan_product where kode = 'KTA'", Integer.class);
    }

    @Test
    @DisplayName("MARKETING boleh membaca pengajuan tetapi tidak boleh menyetujui")
    void marketingHanyaMembaca() throws Exception {
        Integer id = pengajuanTerkirim();

        mockMvc.perform(get(PENGAJUAN).header(HttpHeaders.AUTHORIZATION, bearer(MARKETING)))
                .andExpect(status().isOk());

        mockMvc.perform(post(PENGAJUAN + "/" + id + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, bearer(MARKETING)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("HR tidak memegang satu pun permission pinjaman")
    void hrTidakMenyentuhModulPinjaman() throws Exception {
        mockMvc.perform(get(PENGAJUAN).header(HttpHeaders.AUTHORIZATION, bearer(HR)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/branch").header(HttpHeaders.AUTHORIZATION, bearer(HR)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MANAGER boleh menyetujui tetapi tidak boleh mencairkan")
    void managerMenyetujuiTapiTidakMencairkan() throws Exception {
        Integer id = pengajuanTerkirim();

        mockMvc.perform(post(PENGAJUAN + "/" + id + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, bearer(MANAGER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(post(PENGAJUAN + "/" + id + "/disburse")
                        .header(HttpHeaders.AUTHORIZATION, bearer(MANAGER)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(PENGAJUAN + "/" + id + "/disburse")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISBURSED"));
    }

    @Test
    @DisplayName("SALES boleh mencatat pembayaran, MARKETING tidak")
    void hanyaSalesMencatatPembayaran() throws Exception {
        mockMvc.perform(post("/api/loan-payment")
                        .header(HttpHeaders.AUTHORIZATION, bearer(MARKETING))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badanPembayaran(1)))
                .andExpect(status().isForbidden());

        Integer id = pengajuanCair();
        mockMvc.perform(post("/api/loan-payment")
                        .header(HttpHeaders.AUTHORIZATION, bearer(SALES))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badanPembayaranUntuk(id, 1)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("customer tidak bisa menyentuh endpoint pemrosesan milik pegawai")
    void customerTidakMasukEndpointPegawai() throws Exception {
        Integer id = pengajuanTerkirim();

        mockMvc.perform(get(PENGAJUAN).header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SATU)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(PENGAJUAN + "/" + id + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SATU)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("pegawai tidak bisa memakai endpoint pengajuan milik customer")
    void pegawaiTidakMasukEndpointCustomer() throws Exception {
        mockMvc.perform(get(PENGAJUAN_CUSTOMER).header(HttpHeaders.AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("customer hanya melihat pengajuan miliknya sendiri")
    void customerHanyaMelihatMiliknya() throws Exception {
        Integer id = pengajuanTerkirim();

        mockMvc.perform(get(PENGAJUAN_CUSTOMER + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SATU)))
                .andExpect(status().isOk());

        mockMvc.perform(get(PENGAJUAN_CUSTOMER + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_DUA)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("customer membuat pengajuannya sendiri, pemiliknya diambil dari token")
    void customerMembuatPengajuanSendiri() throws Exception {
        mockMvc.perform(post(PENGAJUAN_CUSTOMER)
                        .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SATU))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idLoanProduct\":" + idProdukKta
                                + ",\"jumlahPengajuan\":10000000.00,\"tenorBulan\":12}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.namaCustomer").value("Customer Satu"));
    }

    @Test
    @DisplayName("tanpa token seluruh endpoint pinjaman dibalas 401")
    void tanpaTokenDitolak() throws Exception {
        mockMvc.perform(get(PENGAJUAN)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(PENGAJUAN_CUSTOMER)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/branch")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("master pinjaman hanya bisa diubah pemegang permission WRITE")
    void masterPinjamanDijagaPermissionWrite() throws Exception {
        mockMvc.perform(post("/api/branch")
                        .header(HttpHeaders.AUTHORIZATION, bearer(SALES))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kode\":\"BR99\",\"nama\":\"Cabang Uji\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/branch")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kode\":\"BR99\",\"nama\":\"Cabang Uji\"}"))
                .andExpect(status().isCreated());
    }

    private Integer pengajuanTerkirim() {
        LoanApplicationResponse draft = loanApplicationService.buatDraft(
                CUSTOMER_SATU, new LoanApplicationRequest(idProdukKta, null, SEPULUH_JUTA, 12));
        loanApplicationService.ajukan(CUSTOMER_SATU, draft.getId());
        return draft.getId();
    }

    private Integer pengajuanCair() {
        Integer id = pengajuanTerkirim();
        loanApplicationService.setujui(id, null);
        loanApplicationService.cairkan(id);
        return id;
    }

    private String badanPembayaran(int angsuranKe) {
        return badanPembayaranUntuk(1, angsuranKe);
    }

    private String badanPembayaranUntuk(Integer idPengajuan, int angsuranKe) {
        return "{\"idLoanApplication\":" + idPengajuan + ",\"angsuranKe\":" + angsuranKe
                + ",\"jumlahBayar\":1000000.00,\"tanggalBayar\":\"2026-08-23\",\"metode\":\"TRANSFER\"}";
    }

    private String bearer(String email) {
        AppUser user = userDetailsService.find(email).orElseThrow();
        return "Bearer " + jwtService.issue(user, Instant.now());
    }
}
