package com.masesas.exercises.demo1.model;

import java.time.LocalDate;

/** Bantuan kecil untuk mapping hasil stored procedure. */
final class SpRowUtils {

    /** Kolom tanggal bisa datang sebagai LocalDate (Hibernate) atau java.sql.Date (driver JDBC). */
    static LocalDate toLocalDate(Object nilai) {
        if (nilai == null) {
            return null;
        }
        return nilai instanceof java.sql.Date tanggal ? tanggal.toLocalDate() : (LocalDate) nilai;
    }

    private SpRowUtils() {
    }
}
