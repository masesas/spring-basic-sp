package com.masesas.exercises.demo1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "loan_product", schema = "masesas")
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "kode")
    private String kode;

    @Column(name = "nama")
    private String nama;

    @Column(name = "bunga_persen")
    private BigDecimal bungaPersen;

    @Column(name = "tenor_min")
    private Integer tenorMin;

    @Column(name = "tenor_max")
    private Integer tenorMax;

    @Column(name = "plafond_min")
    private BigDecimal plafondMin;

    @Column(name = "plafond_max")
    private BigDecimal plafondMax;

    @Column(name = "aktif")
    private Boolean aktif;

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "updated_date")
    private Instant updatedDate;

    @Column(name = "deleted_date")
    private Instant deletedDate;
}
