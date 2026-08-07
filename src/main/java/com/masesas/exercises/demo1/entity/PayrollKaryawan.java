package com.masesas.exercises.demo1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Slip gaji satu karyawan untuk satu periode bulanan.
 *
 * <p>Entity ini memakai tiga anotasi sekaligus:
 * <ul>
 *   <li>{@code @EmbeddedId} — primary key komposit {@link PayrollId}, tanpa surrogate id.</li>
 *   <li>{@code @MapsId} — menyambungkan relasi ke {@link Karyawan} dengan bagian
 *       {@code idKaryawan} dari primary key, sehingga kolom {@code id_karyawan}
 *       berperan ganda sebagai FK sekaligus bagian PK tanpa terduplikasi.</li>
 *   <li>{@code @Embedded} — {@link KomponenGaji} untuk nominalnya.</li>
 * </ul>
 *
 * <p>Tidak ada soft delete di sini: catatan finansial diperbaiki lewat update,
 * dan {@code deleted_date} akan bertabrakan dengan composite primary key.
 */
@Getter
@Setter
@Entity
@Table(name = "payroll_karyawan", schema = "masesas")
public class PayrollKaryawan {

    @EmbeddedId
    private PayrollId id;

    /**
     * {@code @MapsId("idKaryawan")} memberi tahu Hibernate bahwa kolom join di sini
     * adalah kolom yang sama dengan bagian {@code idKaryawan} pada primary key.
     * Karena itu cukup set relasi ini — bagian PK-nya terisi otomatis saat flush.
     */
    @MapsId("idKaryawan")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_karyawan")
    private Karyawan karyawan;

    @Embedded
    private KomponenGaji komponen = KomponenGaji.kosong();

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "updated_date")
    private Instant updatedDate;

    /** Membuat slip gaji baru; bagian {@code idKaryawan} diisi Hibernate lewat {@code @MapsId}. */
    public static PayrollKaryawan baru(
            Karyawan karyawan, LocalDate periode, KomponenGaji komponen, Instant timestamp) {
        PayrollKaryawan payroll = new PayrollKaryawan();
        payroll.id = PayrollId.untukPeriode(periode);
        payroll.karyawan = karyawan;
        payroll.komponen = komponen;
        payroll.createdDate = timestamp;
        payroll.updatedDate = timestamp;
        return payroll;
    }

    /** Merevisi nominal slip gaji — primary key tidak pernah ikut berubah. */
    public void revisi(KomponenGaji komponenBaru, Instant timestamp) {
        this.komponen = komponenBaru;
        this.updatedDate = timestamp;
    }

    /** Hibernate mengisi field embedded dengan {@code null} bila semua kolomnya NULL. */
    public KomponenGaji getKomponen() {
        return komponen == null ? KomponenGaji.kosong() : komponen;
    }

    public LocalDate getPeriode() {
        return id == null ? null : id.getPeriode();
    }
}
