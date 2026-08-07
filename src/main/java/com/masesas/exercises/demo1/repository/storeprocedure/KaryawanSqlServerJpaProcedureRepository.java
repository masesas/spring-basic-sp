package com.masesas.exercises.demo1.repository.storeprocedure;

import com.masesas.exercises.demo1.model.HasilProsesKaryawan;
import com.masesas.exercises.demo1.model.KaryawanLengkap;
import com.masesas.exercises.demo1.model.RekeningRingkas;
import com.masesas.exercises.demo1.model.TrainingRingkas;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Pemanggil sp_proses_karyawan (SQL Server) lewat JPA StoredProcedureQuery. */
@Repository
public class KaryawanSqlServerJpaProcedureRepository {

    // ganti ke EntityManager milik persistence unit SQL Server kalau koneksinya dipisah
    @PersistenceContext
    private EntityManager entityManager;

    /** Tiga result set diambil berurutan memakai hasMoreResults(). */
    @Transactional
    @SuppressWarnings("unchecked")
    public HasilProsesKaryawan proses(Integer id, String nama, String alamat, String status, String mode) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("masesas.sp_proses_karyawan")
                .registerStoredProcedureParameter("id", Integer.class, ParameterMode.IN)
                .registerStoredProcedureParameter("nama", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("alamat", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("status", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("mode", String.class, ParameterMode.IN);

        query.setParameter("id", id);
        query.setParameter("nama", nama);
        query.setParameter("alamat", alamat);
        query.setParameter("status", status);
        query.setParameter("mode", mode);
        query.execute();

        // result set diambil berurutan: karyawan, rekening, lalu training
        List<Object[]> barisKaryawan = query.getResultList();
        query.hasMoreResults();
        List<Object[]> barisRekening = query.getResultList();
        query.hasMoreResults();
        List<Object[]> barisTraining = query.getResultList();

        return new HasilProsesKaryawan(
                barisKaryawan.isEmpty() ? null : KaryawanLengkap.fromRow(barisKaryawan.getFirst()),
                barisRekening.stream().map(RekeningRingkas::fromRow).toList(),
                barisTraining.stream().map(TrainingRingkas::fromRow).toList());
    }
}
