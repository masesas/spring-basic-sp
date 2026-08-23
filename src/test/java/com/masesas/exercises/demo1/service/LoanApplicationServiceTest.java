package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.LoanApplicationRequest;
import com.masesas.exercises.demo1.dto.LoanApplicationResponse;
import com.masesas.exercises.demo1.dto.LoanPaymentRequest;
import com.masesas.exercises.demo1.exception.BusinessRuleException;
import com.masesas.exercises.demo1.exception.InvalidRequestException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LoanApplicationServiceTest {

    private static final String CUSTOMER_SATU = "customer1@masesas.test";
    private static final String CUSTOMER_DUA = "customer2@masesas.test";
    private static final BigDecimal SEPULUH_JUTA = new BigDecimal("10000000.00");

    @Autowired
    private LoanApplicationService loanApplicationService;

    @Autowired
    private LoanPaymentService loanPaymentService;

    @Autowired
    private LoanPlafondService loanPlafondService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer idProdukKta;
    private Integer idCustomerSatu;

    @BeforeEach
    void ambilDataAcuan() {
        idProdukKta = jdbcTemplate.queryForObject(
                "select id from masesas.loan_product where kode = 'KTA'", Integer.class);
        idCustomerSatu = jdbcTemplate.queryForObject(
                "select id from masesas.customer where email = ?", Integer.class, CUSTOMER_SATU);
    }

    @Test
    @DisplayName("alur lengkap DRAFT sampai DISBURSED memakai plafond customer")
    void alurLengkapSampaiCair() {
        LoanApplicationResponse draft = buatDraft(CUSTOMER_SATU, SEPULUH_JUTA, 12);
        assertThat(draft.getStatus()).isEqualTo("DRAFT");

        assertThat(loanApplicationService.ajukan(CUSTOMER_SATU, draft.getId()).getStatus())
                .isEqualTo("SUBMITTED");
        assertThat(loanApplicationService.setujui(draft.getId(), "lolos verifikasi").getStatus())
                .isEqualTo("APPROVED");

        BigDecimal sisaSebelum = loanPlafondService.findByCustomer(idCustomerSatu).getSisa();

        assertThat(loanApplicationService.cairkan(draft.getId()).getStatus()).isEqualTo("DISBURSED");
        assertThat(loanPlafondService.findByCustomer(idCustomerSatu).getSisa())
                .isEqualByComparingTo(sisaSebelum.subtract(SEPULUH_JUTA));
    }

    @Test
    @DisplayName("pembayaran angsuran mengembalikan sebagian plafond terpakai")
    void pembayaranMengembalikanPlafond() {
        LoanApplicationResponse pengajuan = sampaiCair();
        BigDecimal terpakaiSebelum =
                loanPlafondService.findByCustomer(idCustomerSatu).getPlafondTerpakai();

        loanPaymentService.catat(new LoanPaymentRequest(
                pengajuan.getId(), 1, new BigDecimal("4000000.00"), LocalDate.now(), "TRANSFER"));

        assertThat(loanPlafondService.findByCustomer(idCustomerSatu).getPlafondTerpakai())
                .isEqualByComparingTo(terpakaiSebelum.subtract(new BigDecimal("4000000.00")));
        assertThat(loanPaymentService.daftarPerPengajuan(pengajuan.getId())).hasSize(1);
    }

    @Test
    @DisplayName("pengajuan tidak bisa disetujui langsung dari DRAFT")
    void tidakBisaSetujuiDariDraft() {
        LoanApplicationResponse draft = buatDraft(CUSTOMER_SATU, SEPULUH_JUTA, 12);

        assertThatThrownBy(() -> loanApplicationService.setujui(draft.getId(), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SUBMITTED");
    }

    @Test
    @DisplayName("pengajuan yang sudah dikirim tidak bisa direvisi lagi")
    void tidakBisaRevisiSetelahDikirim() {
        LoanApplicationResponse draft = buatDraft(CUSTOMER_SATU, SEPULUH_JUTA, 12);
        loanApplicationService.ajukan(CUSTOMER_SATU, draft.getId());

        assertThatThrownBy(() -> loanApplicationService.revisiDraft(
                CUSTOMER_SATU, draft.getId(), permintaan(SEPULUH_JUTA, 24)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("nominal di luar rentang produk ditolak sebelum tersimpan")
    void nominalDiLuarRentangProdukDitolak() {
        assertThatThrownBy(() -> buatDraft(CUSTOMER_SATU, new BigDecimal("1000000.00"), 12))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("jumlah pengajuan");
    }

    @Test
    @DisplayName("tenor di luar rentang produk ditolak sebelum tersimpan")
    void tenorDiLuarRentangProdukDitolak() {
        assertThatThrownBy(() -> buatDraft(CUSTOMER_SATU, SEPULUH_JUTA, 60))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("tenor");
    }

    @Test
    @DisplayName("persetujuan ditolak bila jumlah melebihi sisa plafond customer")
    void melebihiSisaPlafondDitolak() {
        LoanApplicationResponse draft = buatDraft(CUSTOMER_DUA, new BigDecimal("40000000.00"), 12);
        loanApplicationService.ajukan(CUSTOMER_DUA, draft.getId());

        assertThatThrownBy(() -> loanApplicationService.setujui(draft.getId(), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("sisa plafond");
    }

    @Test
    @DisplayName("customer tidak bisa membaca pengajuan milik customer lain")
    void pengajuanCustomerLainTidakTerlihat() {
        LoanApplicationResponse milikSatu = buatDraft(CUSTOMER_SATU, SEPULUH_JUTA, 12);

        assertThatThrownBy(() ->
                loanApplicationService.detailMilikCustomer(CUSTOMER_DUA, milikSatu.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("pembayaran hanya bisa dicatat untuk pinjaman yang sudah dicairkan")
    void pembayaranSebelumCairDitolak() {
        LoanApplicationResponse draft = buatDraft(CUSTOMER_SATU, SEPULUH_JUTA, 12);

        assertThatThrownBy(() -> loanPaymentService.catat(new LoanPaymentRequest(
                draft.getId(), 1, new BigDecimal("1000000.00"), LocalDate.now(), "TRANSFER")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DISBURSED");
    }

    @Test
    @DisplayName("total pembayaran tidak boleh melebihi nilai pinjaman")
    void pembayaranMelebihiPinjamanDitolak() {
        LoanApplicationResponse pengajuan = sampaiCair();

        assertThatThrownBy(() -> loanPaymentService.catat(new LoanPaymentRequest(
                pengajuan.getId(), 1, new BigDecimal("11000000.00"), LocalDate.now(), "TRANSFER")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("melebihi nilai pinjaman");
    }

    @Test
    @DisplayName("pengajuan yang sudah dibatalkan tidak bisa dikirim lagi")
    void pengajuanBatalTidakBisaDikirim() {
        LoanApplicationResponse draft = buatDraft(CUSTOMER_SATU, SEPULUH_JUTA, 12);
        assertThat(loanApplicationService.batalkan(CUSTOMER_SATU, draft.getId()).getStatus())
                .isEqualTo("CANCELLED");

        assertThatThrownBy(() -> loanApplicationService.ajukan(CUSTOMER_SATU, draft.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("penolakan wajib menyertakan alasan")
    void penolakanTanpaAlasanDitolak() {
        LoanApplicationResponse draft = buatDraft(CUSTOMER_SATU, SEPULUH_JUTA, 12);
        loanApplicationService.ajukan(CUSTOMER_SATU, draft.getId());

        assertThatThrownBy(() -> loanApplicationService.tolak(draft.getId(), "  "))
                .isInstanceOf(InvalidRequestException.class);

        assertThat(loanApplicationService.tolak(draft.getId(), "penghasilan kurang").getStatus())
                .isEqualTo("REJECTED");
    }

    private LoanApplicationResponse sampaiCair() {
        LoanApplicationResponse draft = buatDraft(CUSTOMER_SATU, SEPULUH_JUTA, 12);
        loanApplicationService.ajukan(CUSTOMER_SATU, draft.getId());
        loanApplicationService.setujui(draft.getId(), null);
        return loanApplicationService.cairkan(draft.getId());
    }

    private LoanApplicationResponse buatDraft(String email, BigDecimal jumlah, int tenor) {
        return loanApplicationService.buatDraft(email, permintaan(jumlah, tenor));
    }

    private LoanApplicationRequest permintaan(BigDecimal jumlah, int tenor) {
        return new LoanApplicationRequest(idProdukKta, null, jumlah, tenor);
    }
}
