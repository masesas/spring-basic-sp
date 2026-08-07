package com.masesas.exercises.demo1.repository.storeprocedure;

import com.masesas.exercises.demo1.model.HasilProsesKaryawan;
import com.masesas.exercises.demo1.model.KaryawanLengkap;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Pemanggil sp_proses_karyawan (PostgreSQL) lewat JdbcTemplate. */
@Repository
@RequiredArgsConstructor
public class KaryawanPostgresProcedureRepository {

    // nama cursor harus sama dengan nilai default parameter INOUT di stored procedure
    private static final String CUR_KARYAWAN = "cur_karyawan";
    private static final String CUR_REKENING = "cur_rekening";
    private static final String CUR_TRAINING = "cur_training";

    // setiap parameter di-CAST supaya tipenya cocok walaupun nilainya null
    private static final String SQL_CALL = """
            CALL masesas.sp_proses_karyawan(
                CAST(? AS INTEGER), CAST(? AS VARCHAR), CAST(? AS VARCHAR),
                CAST(? AS VARCHAR), CAST(? AS VARCHAR),
                CAST(? AS REFCURSOR), CAST(? AS REFCURSOR), CAST(? AS REFCURSOR))
            """;

    private final JdbcTemplate jdbcTemplate;

    /** Wajib @Transactional: ketiga cursor otomatis tertutup begitu transaksi selesai. */
    @Transactional
    public HasilProsesKaryawan proses(Integer id, String nama, String alamat, String status, String mode) {
        // langkah 1: CALL menjalankan logika SP dan membuka tiga cursor sekaligus
        jdbcTemplate.query(
                SQL_CALL,
                rs -> null,
                id, nama, alamat, status, mode, CUR_KARYAWAN, CUR_REKENING, CUR_TRAINING);

        // langkah 2: baca isi tiap cursor, sama seperti FETCH ALL IN di psql
        List<KaryawanLengkap> karyawan = jdbcTemplate.query(
                "FETCH ALL IN " + CUR_KARYAWAN, SpRowMappers.KARYAWAN);

        return new HasilProsesKaryawan(
                karyawan.isEmpty() ? null : karyawan.getFirst(),
                jdbcTemplate.query("FETCH ALL IN " + CUR_REKENING, SpRowMappers.REKENING),
                jdbcTemplate.query("FETCH ALL IN " + CUR_TRAINING, SpRowMappers.TRAINING));
    }
}
