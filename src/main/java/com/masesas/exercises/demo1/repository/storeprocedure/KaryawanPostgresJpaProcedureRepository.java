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

/** Pemanggil sp_proses_karyawan (PostgreSQL) lewat JPA StoredProcedureQuery. */
@Repository
public class KaryawanPostgresJpaProcedureRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /** Wajib @Transactional: ketiga cursor otomatis tertutup begitu transaksi selesai. */
    @Transactional
    @SuppressWarnings("unchecked")
    public HasilProsesKaryawan proses(Integer id, String nama, String alamat, String status, String mode) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("masesas.sp_proses_karyawan")
                .registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN)
                .registerStoredProcedureParameter(2, String.class, ParameterMode.IN)
                .registerStoredProcedureParameter(3, String.class, ParameterMode.IN)
                .registerStoredProcedureParameter(4, String.class, ParameterMode.IN)
                .registerStoredProcedureParameter(5, String.class, ParameterMode.IN)
                // tiga REF_CURSOR di bawah ini adalah tiga result set yang dikembalikan
                .registerStoredProcedureParameter(6, void.class, ParameterMode.REF_CURSOR)
                .registerStoredProcedureParameter(7, void.class, ParameterMode.REF_CURSOR)
                .registerStoredProcedureParameter(8, void.class, ParameterMode.REF_CURSOR);

        query.setParameter(1, id);
        query.setParameter(2, nama);
        query.setParameter(3, alamat);
        query.setParameter(4, status);
        query.setParameter(5, mode);
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
