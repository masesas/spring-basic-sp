package com.masesas.exercises.demo1.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "karyawan_training", schema = "masesas")
public class KaryawanTraining {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "tanggal")
    private LocalDate tanggal;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "id_karyawan")
    private Karyawan idKaryawan;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "id_training")
    private Training idTraining;

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "deleted_date")
    private Instant deletedDate;

    @Column(name = "updated_date")
    private Instant updatedDate;


}