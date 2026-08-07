package com.masesas.exercises.demo1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * Tiga kolom jejak waktu yang dipakai berulang oleh entity di skema ini.
 *
 * <p>Sebagai {@link Embeddable}, kelas ini bukan tabel tersendiri: kolomnya ikut menempel
 * di tabel entity pemiliknya, jadi tidak ada perubahan skema database sama sekali.
 *
 * <p>Nilainya diperlakukan sebagai value object yang tidak dimutasi — setiap perubahan
 * menghasilkan instance baru lewat {@link #createdAt}, {@link #touched}, dan {@link #deletedAt}.
 */
@Getter
@EqualsAndHashCode
@ToString
@Embeddable
public class AuditDates {

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "updated_date")
    private Instant updatedDate;

    @Column(name = "deleted_date")
    private Instant deletedDate;

    /** Dipakai Hibernate saat memuat baris dari database. */
    protected AuditDates() {
    }

    private AuditDates(Instant createdDate, Instant updatedDate, Instant deletedDate) {
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.deletedDate = deletedDate;
    }

    /** Jejak untuk baris yang baru dibuat: created dan updated diisi waktu yang sama. */
    public static AuditDates createdAt(Instant timestamp) {
        return new AuditDates(timestamp, timestamp, null);
    }

    /** Jejak kosong, dipakai sebagai nilai awal sebelum entity disimpan. */
    public static AuditDates empty() {
        return new AuditDates(null, null, null);
    }

    /** Salinan baru dengan updated_date diperbarui. */
    public AuditDates touched(Instant timestamp) {
        return new AuditDates(createdDate, timestamp, deletedDate);
    }

    /** Salinan baru yang ditandai terhapus (soft delete) sekaligus memperbarui updated_date. */
    public AuditDates deletedAt(Instant timestamp) {
        return new AuditDates(createdDate, timestamp, timestamp);
    }

    public boolean isDeleted() {
        return deletedDate != null;
    }
}
