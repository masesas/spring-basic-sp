package com.masesas.exercises.demo1.entity;

import com.masesas.exercises.demo1.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "loan_application", schema = "masesas")
public class LoanApplication {

    private static final List<StatusLoanApplication> BISA_DIBATALKAN =
            List.of(StatusLoanApplication.DRAFT, StatusLoanApplication.SUBMITTED);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_customer")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_loan_product")
    private LoanProduct loanProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_branch")
    private Branch branch;

    @Column(name = "jumlah_pengajuan")
    private BigDecimal jumlahPengajuan;

    @Column(name = "tenor_bulan")
    private Integer tenorBulan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusLoanApplication status = StatusLoanApplication.DRAFT;

    @Column(name = "catatan")
    private String catatan;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "updated_date")
    private Instant updatedDate;

    public static LoanApplication baru(
            Customer customer,
            LoanProduct loanProduct,
            Branch branch,
            BigDecimal jumlahPengajuan,
            Integer tenorBulan,
            Instant timestamp) {
        LoanApplication pengajuan = new LoanApplication();
        pengajuan.customer = customer;
        pengajuan.loanProduct = loanProduct;
        pengajuan.branch = branch;
        pengajuan.jumlahPengajuan = jumlahPengajuan;
        pengajuan.tenorBulan = tenorBulan;
        pengajuan.status = StatusLoanApplication.DRAFT;
        pengajuan.createdDate = timestamp;
        pengajuan.updatedDate = timestamp;
        return pengajuan;
    }

    public void revisi(
            LoanProduct loanProductBaru,
            Branch branchBaru,
            BigDecimal jumlahBaru,
            Integer tenorBaru,
            Instant timestamp) {
        wajibBerstatus(StatusLoanApplication.DRAFT, "direvisi");
        this.loanProduct = loanProductBaru;
        this.branch = branchBaru;
        this.jumlahPengajuan = jumlahBaru;
        this.tenorBulan = tenorBaru;
        this.updatedDate = timestamp;
    }

    public void ajukan(Instant timestamp) {
        wajibBerstatus(StatusLoanApplication.DRAFT, "diajukan");
        pindahKe(StatusLoanApplication.SUBMITTED, timestamp);
    }

    public void setujui(String catatanKeputusan, Instant timestamp) {
        wajibBerstatus(StatusLoanApplication.SUBMITTED, "disetujui");
        this.catatan = catatanKeputusan;
        pindahKe(StatusLoanApplication.APPROVED, timestamp);
    }

    public void tolak(String alasan, Instant timestamp) {
        wajibBerstatus(StatusLoanApplication.SUBMITTED, "ditolak");
        this.catatan = alasan;
        pindahKe(StatusLoanApplication.REJECTED, timestamp);
    }

    public void cairkan(Instant timestamp) {
        wajibBerstatus(StatusLoanApplication.APPROVED, "dicairkan");
        pindahKe(StatusLoanApplication.DISBURSED, timestamp);
    }

    public void batalkan(Instant timestamp) {
        if (!BISA_DIBATALKAN.contains(status)) {
            throw new BusinessRuleException(
                    "Pengajuan berstatus " + status + " tidak bisa dibatalkan");
        }
        pindahKe(StatusLoanApplication.CANCELLED, timestamp);
    }

    public boolean dimilikiOleh(Integer idCustomer) {
        return customer != null && customer.getId().equals(idCustomer);
    }

    private void pindahKe(StatusLoanApplication statusBaru, Instant timestamp) {
        this.status = statusBaru;
        this.updatedDate = timestamp;
    }

    private void wajibBerstatus(StatusLoanApplication diharapkan, String aksi) {
        if (status != diharapkan) {
            throw new BusinessRuleException(
                    "Pengajuan hanya bisa " + aksi + " saat berstatus " + diharapkan
                            + ", status saat ini " + status);
        }
    }
}
