package com.masesas.exercises.demo1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Composite primary key untuk {@link PayrollKaryawan}: satu slip gaji diidentifikasi
 * oleh pasangan (karyawan, periode).
 *
 * <p>Dua syarat wajib untuk kelas {@code @EmbeddedId}, keduanya bukan formalitas:
 * <ul>
 *   <li>{@link Serializable} — dituntut spesifikasi JPA untuk tipe primary key.</li>
 *   <li>{@code equals}/{@code hashCode} — Hibernate memakainya sebagai identitas entity
 *       di persistence context. Tanpa ini, entity yang sama bisa ter-load dua kali dan
 *       {@code merge()} menghasilkan baris duplikat, bukan update.</li>
 * </ul>
 *
 * <p>Bagian {@code idKaryawan} tidak diisi manual — {@code @MapsId} pada
 * {@link PayrollKaryawan#getKaryawan()} yang menurunkannya dari relasi saat flush.
 */
@Getter
@EqualsAndHashCode
@ToString
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class PayrollId implements Serializable {

    @Column(name = "id_karyawan")
    private Integer idKaryawan;

    /** Selalu tanggal 1 setiap bulan — dijaga CHECK constraint di database. */
    @Column(name = "periode")
    private LocalDate periode;

    /** Key yang bagian karyawannya masih kosong, menunggu diisi {@code @MapsId}. */
    public static PayrollId untukPeriode(LocalDate periode) {
        return new PayrollId(null, periode);
    }
}
