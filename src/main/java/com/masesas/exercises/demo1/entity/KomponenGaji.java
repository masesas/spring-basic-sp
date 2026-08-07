package com.masesas.exercises.demo1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Rincian nominal pada satu slip gaji.
 *
 * <p>Sebagai {@link Embeddable}, ketiga kolomnya menempel di tabel payroll_karyawan —
 * bukan tabel terpisah. Keuntungan memisahkannya ke kelas sendiri: perilaku yang
 * berkaitan dengan angka-angka ini ({@link #bersih()}) ikut tinggal bersama datanya,
 * bukan tersebar di service.
 *
 * <p>Immutable — setiap perubahan menghasilkan instance baru.
 */
@Getter
@EqualsAndHashCode
@ToString
@Embeddable
public class KomponenGaji {

    @Column(name = "gaji_pokok")
    private BigDecimal gajiPokok;

    @Column(name = "tunjangan")
    private BigDecimal tunjangan;

    @Column(name = "potongan")
    private BigDecimal potongan;

    /** Dipakai Hibernate saat memuat baris dari database. */
    protected KomponenGaji() {
    }

    private KomponenGaji(BigDecimal gajiPokok, BigDecimal tunjangan, BigDecimal potongan) {
        this.gajiPokok = gajiPokok;
        this.tunjangan = tunjangan;
        this.potongan = potongan;
    }

    /** Nilai {@code null} diperlakukan sebagai nol agar {@link #bersih()} selalu aman dihitung. */
    public static KomponenGaji of(BigDecimal gajiPokok, BigDecimal tunjangan, BigDecimal potongan) {
        return new KomponenGaji(nolJikaNull(gajiPokok), nolJikaNull(tunjangan), nolJikaNull(potongan));
    }

    public static KomponenGaji kosong() {
        return new KomponenGaji(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** Gaji bersih = gaji pokok + tunjangan - potongan. */
    public BigDecimal bersih() {
        return nolJikaNull(gajiPokok)
                .add(nolJikaNull(tunjangan))
                .subtract(nolJikaNull(potongan));
    }

    /** Total penghasilan sebelum potongan. */
    public BigDecimal bruto() {
        return nolJikaNull(gajiPokok).add(nolJikaNull(tunjangan));
    }

    private static BigDecimal nolJikaNull(BigDecimal nilai) {
        return nilai == null ? BigDecimal.ZERO : nilai;
    }
}
