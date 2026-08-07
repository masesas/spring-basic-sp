package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.DetailKaryawan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DetailKaryawanRepository extends JpaRepository<DetailKaryawan, Integer> {

    Optional<DetailKaryawan> findByIdAndDeletedDateIsNull(Integer id);

    Page<DetailKaryawan> findAllByDeletedDateIsNull(Pageable pageable);

    boolean existsByNikAndDeletedDateIsNull(String nik);

    boolean existsByNikAndDeletedDateIsNullAndIdNot(String nik, Integer id);

    boolean existsByNpwpAndDeletedDateIsNull(String npwp);

    boolean existsByNpwpAndDeletedDateIsNullAndIdNot(String npwp, Integer id);
}
