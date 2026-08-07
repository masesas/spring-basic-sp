package com.masesas.exercises.demo1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "training", schema = "masesas")
public class Training {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "tema")
    private String tema;

    @Column(name = "pengajar")
    private String pengajar;

    /**
     * created_date, updated_date, dan deleted_date dipindahkan ke {@link AuditDates}.
     * Kolomnya tetap berada di tabel training — {@link Embedded} tidak mengubah skema.
     */
    @Embedded
    private AuditDates audit = AuditDates.empty();

    /**
     * Hibernate mengisi field ini dengan {@code null} bila ketiga kolom audit bernilai NULL,
     * sehingga pemanggil selalu menerima instance yang aman dipakai.
     */
    public AuditDates getAudit() {
        return audit == null ? AuditDates.empty() : audit;
    }
}
