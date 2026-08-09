package com.masesas.exercises.demo1.entity;

import com.masesas.exercises.demo1.owasp.safe.CryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "detail_karyawan", schema = "masesas")
public class DetailKaryawan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Convert(converter = CryptoConverter.class)
    @Column(name = "nik", nullable = false)
    private String nik;

    @Convert(converter = CryptoConverter.class)
    @Column(name = "npwp", nullable = false)
    private String npwp;

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "deleted_date")
    private Instant deletedDate;

    @Column(name = "updated_date")
    private Instant updatedDate;


}