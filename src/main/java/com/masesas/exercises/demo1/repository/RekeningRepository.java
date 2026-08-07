package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.Rekening;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RekeningRepository extends JpaRepository<Rekening, Integer> {

    @EntityGraph(attributePaths = "idKaryawan")
    Optional<Rekening> findByIdAndDeletedDateIsNull(Integer id);

    @EntityGraph(attributePaths = "idKaryawan")
    Page<Rekening> findAllByDeletedDateIsNull(Pageable pageable);

    @EntityGraph(attributePaths = "idKaryawan")
    List<Rekening> findAllByIdKaryawan_IdAndDeletedDateIsNullOrderByIdAsc(Integer karyawanId);

    boolean existsByJenisIgnoreCaseAndRekeningAndDeletedDateIsNull(String jenis, String rekening);

    boolean existsByJenisIgnoreCaseAndRekeningAndDeletedDateIsNullAndIdNot(String jenis, String rekening, Integer id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Rekening r set r.deletedDate = :timestamp, r.updatedDate = :timestamp "
            + "where r.idKaryawan.id = :karyawanId and r.deletedDate is null")
    int softDeleteByKaryawanId(@Param("karyawanId") Integer karyawanId, @Param("timestamp") Instant timestamp);
}
