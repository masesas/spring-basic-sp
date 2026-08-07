package com.masesas.exercises.demo1.repository.storeprocedure;

import com.masesas.exercises.demo1.model.KaryawanLengkap;
import com.masesas.exercises.demo1.model.RekeningRingkas;
import com.masesas.exercises.demo1.model.TrainingRingkas;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDate;

/** RowMapper untuk ketiga result set sp_proses_karyawan; dipakai repository JdbcTemplate. */
final class SpRowMappers {

    static final RowMapper<KaryawanLengkap> KARYAWAN = (rs, rowNum) -> new KaryawanLengkap(
            rs.getInt("id"),
            rs.getString("nama"),
            rs.getString("alamat"),
            rs.getString("status"),
            rs.getObject("dob", LocalDate.class),
            rs.getInt("umur"),
            rs.getString("kategori_umur"),
            rs.getString("nik"),
            rs.getString("npwp"),
            rs.getInt("jumlah_rekening"));

    static final RowMapper<RekeningRingkas> REKENING = (rs, rowNum) -> new RekeningRingkas(
            rs.getInt("id"),
            rs.getString("nama"),
            rs.getString("jenis"),
            rs.getString("rekening"));

    static final RowMapper<TrainingRingkas> TRAINING = (rs, rowNum) -> new TrainingRingkas(
            rs.getInt("id"),
            rs.getString("tema"),
            rs.getString("pengajar"),
            rs.getObject("tanggal", LocalDate.class));

    private SpRowMappers() {
    }
}
