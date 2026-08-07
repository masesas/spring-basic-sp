package com.masesas.exercises.demo1.entity;

import jakarta.persistence.Column;
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

    @Column(name = "nik", nullable = false)
    private String nik;

    @Column(name = "npwp", nullable = false)
    private String npwp;

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "deleted_date")
    private Instant deletedDate;

    @Column(name = "updated_date")
    private Instant updatedDate;


}