package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.KaryawanTraining;
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

public interface KaryawanTrainingRepository extends JpaRepository<KaryawanTraining, Integer> {

    @EntityGraph(attributePaths = {"idKaryawan", "idTraining"})
    Optional<KaryawanTraining> findByIdAndDeletedDateIsNull(Integer id);

    @EntityGraph(attributePaths = {"idKaryawan", "idTraining"})
    Page<KaryawanTraining> findAllByDeletedDateIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"idKaryawan", "idTraining"})
    List<KaryawanTraining> findAllByIdKaryawan_IdAndDeletedDateIsNullOrderByTanggalDesc(Integer karyawanId);

    @EntityGraph(attributePaths = {"idKaryawan", "idTraining"})
    List<KaryawanTraining> findAllByIdTraining_IdAndDeletedDateIsNullOrderByTanggalDesc(Integer trainingId);

    boolean existsByIdKaryawan_IdAndIdTraining_IdAndDeletedDateIsNull(Integer karyawanId, Integer trainingId);

    boolean existsByIdKaryawan_IdAndIdTraining_IdAndDeletedDateIsNullAndIdNot(
            Integer karyawanId, Integer trainingId, Integer id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update KaryawanTraining kt set kt.deletedDate = :timestamp, kt.updatedDate = :timestamp "
            + "where kt.idKaryawan.id = :karyawanId and kt.deletedDate is null")
    int softDeleteByKaryawanId(@Param("karyawanId") Integer karyawanId, @Param("timestamp") Instant timestamp);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update KaryawanTraining kt set kt.deletedDate = :timestamp, kt.updatedDate = :timestamp "
            + "where kt.idTraining.id = :trainingId and kt.deletedDate is null")
    int softDeleteByTrainingId(@Param("trainingId") Integer trainingId, @Param("timestamp") Instant timestamp);
}
