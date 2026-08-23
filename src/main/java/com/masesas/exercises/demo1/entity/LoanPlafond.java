package com.masesas.exercises.demo1.entity;

import com.masesas.exercises.demo1.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "loan_plafond", schema = "masesas")
public class LoanPlafond {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_customer")
    private Customer customer;

    @Column(name = "plafond_total")
    private BigDecimal plafondTotal = BigDecimal.ZERO;

    @Column(name = "plafond_terpakai")
    private BigDecimal plafondTerpakai = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "updated_date")
    private Instant updatedDate;

    public static LoanPlafond baru(Customer customer, BigDecimal total, Instant timestamp) {
        LoanPlafond plafond = new LoanPlafond();
        plafond.customer = customer;
        plafond.plafondTotal = total;
        plafond.plafondTerpakai = BigDecimal.ZERO;
        plafond.createdDate = timestamp;
        plafond.updatedDate = timestamp;
        return plafond;
    }

    public BigDecimal sisa() {
        return plafondTotal.subtract(plafondTerpakai);
    }

    public void ubahTotal(BigDecimal totalBaru, Instant timestamp) {
        if (totalBaru.compareTo(plafondTerpakai) < 0) {
            throw new BusinessRuleException(
                    "Plafond total tidak boleh lebih kecil dari plafond yang sudah terpakai ("
                            + plafondTerpakai + ")");
        }
        this.plafondTotal = totalBaru;
        this.updatedDate = timestamp;
    }

    public void pakai(BigDecimal jumlah, Instant timestamp) {
        if (jumlah.compareTo(sisa()) > 0) {
            throw new BusinessRuleException(
                    "Jumlah pinjaman melebihi sisa plafond customer (sisa " + sisa() + ")");
        }
        this.plafondTerpakai = plafondTerpakai.add(jumlah);
        this.updatedDate = timestamp;
    }

    public void kembalikan(BigDecimal jumlah, Instant timestamp) {
        BigDecimal terpakaiBaru = plafondTerpakai.subtract(jumlah);
        this.plafondTerpakai = terpakaiBaru.signum() < 0 ? BigDecimal.ZERO : terpakaiBaru;
        this.updatedDate = timestamp;
    }
}
