package com.masesas.exercises.demo1.repository.storeprocedure;

import com.masesas.exercises.demo1.model.HasilProsesKaryawan;
import com.masesas.exercises.demo1.model.KaryawanLengkap;
import com.masesas.exercises.demo1.model.RekeningRingkas;
import com.masesas.exercises.demo1.model.TrainingRingkas;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Pemanggil sp_proses_karyawan (SQL Server) lewat JdbcTemplate. */
@Repository
@RequiredArgsConstructor
public class KaryawanSqlServerProcedureRepository {

    // ganti ke JdbcTemplate milik datasource SQL Server kalau koneksinya dipisah
    private final JdbcTemplate jdbcTemplate;

    /** Tiga result set dibaca berurutan memakai getMoreResults(). */
    public HasilProsesKaryawan proses(Integer id, String nama, String alamat, String status, String mode) {
        return jdbcTemplate.execute(
                (java.sql.Connection con) -> {
                    CallableStatement cs = con.prepareCall("{call masesas.sp_proses_karyawan(?, ?, ?, ?, ?)}");
                    cs.setObject(1, id, Types.INTEGER);
                    cs.setObject(2, nama, Types.VARCHAR);
                    cs.setObject(3, alamat, Types.VARCHAR);
                    cs.setObject(4, status, Types.VARCHAR);
                    cs.setObject(5, mode, Types.VARCHAR);
                    return cs;
                },
                (CallableStatement cs) -> {
                    cs.execute();

                    List<KaryawanLengkap> karyawan = bacaResultSet(cs.getResultSet(), SpRowMappers.KARYAWAN);

                    cs.getMoreResults();
                    List<RekeningRingkas> rekening = bacaResultSet(cs.getResultSet(), SpRowMappers.REKENING);

                    cs.getMoreResults();
                    List<TrainingRingkas> training = bacaResultSet(cs.getResultSet(), SpRowMappers.TRAINING);

                    return new HasilProsesKaryawan(
                            karyawan.isEmpty() ? null : karyawan.getFirst(), rekening, training);
                });
    }

    /** Ubah satu ResultSet menjadi list memakai RowMapper. */
    private static <T> List<T> bacaResultSet(ResultSet rs, RowMapper<T> mapper) throws java.sql.SQLException {
        List<T> hasil = new ArrayList<>();
        int nomorBaris = 0;
        while (rs != null && rs.next()) {
            hasil.add(mapper.mapRow(rs, nomorBaris++));
        }
        return hasil;
    }
}
