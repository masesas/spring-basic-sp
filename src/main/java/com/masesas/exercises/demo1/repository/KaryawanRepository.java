package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.projection.KaryawanTrainingProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface KaryawanRepository extends JpaRepository<Karyawan, Integer> {

    @EntityGraph(attributePaths = "detailKaryawan")
    Optional<Karyawan> findByIdAndDeletedDateIsNull(Integer id);

    @EntityGraph(attributePaths = "detailKaryawan")
    Page<Karyawan> findAllByDeletedDateIsNull(Pageable pageable);

    /*@EntityGraph(attributePaths = "detailKaryawan")*/
    //Page<Karyawan> findAllByNamaContainingIgnoreCaseAndDeletedDateIsNull(String nama, Pageable pageable);

    @EntityGraph(attributePaths = "detailKaryawan")
    List<Karyawan> findAllByDeletedDateIsNullOrderByIdAsc();

    boolean existsByIdAndDeletedDateIsNull(Integer id);

    boolean existsByDetailKaryawan_IdAndDeletedDateIsNull(Integer detailKaryawanId);


    @Query("""
            SELECT 
                        k.id AS karyawanId, 
                        k.nama, 
                        kt.tanggal as tanggalTraining,
                        k.createdDate as createdDateKaryawan
            FROM Karyawan k 
                        JOIN 
                                    KaryawanTraining kt ON k.id = kt.idKaryawan.id
            """)
    List<KaryawanTrainingProjection> findKaryawanTrainingByKaryawanId();


    // select * from karyawan where nama lower(like) '%{nama}%'
    Page<Karyawan> findAllByNamaContainingIgnoreCaseAndDeletedDateIsNull(String nama, Pageable pageable);
}
