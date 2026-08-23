package com.masesas.exercises.demo1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "loan_payment", schema = "masesas")
public class LoanPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_loan_application")
    private LoanApplication loanApplication;

    @Column(name = "angsuran_ke")
    private Integer angsuranKe;

    @Column(name = "jumlah_bayar")
    private BigDecimal jumlahBayar;

    @Column(name = "tanggal_bayar")
    private LocalDate tanggalBayar;

    @Column(name = "metode")
    private String metode;

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "updated_date")
    private Instant updatedDate;

    public static LoanPayment baru(
            LoanApplication loanApplication,
            Integer angsuranKe,
            BigDecimal jumlahBayar,
            LocalDate tanggalBayar,
            String metode,
            Instant timestamp) {
        LoanPayment payment = new LoanPayment();
        payment.loanApplication = loanApplication;
        payment.angsuranKe = angsuranKe;
        payment.jumlahBayar = jumlahBayar;
        payment.tanggalBayar = tanggalBayar;
        payment.metode = metode;
        payment.createdDate = timestamp;
        payment.updatedDate = timestamp;
        return payment;
    }
}
