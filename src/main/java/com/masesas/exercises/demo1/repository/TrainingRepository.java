package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.Training;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * deletedDate kini berada di dalam embeddable {@code audit}, sehingga derived query
 * menyebutnya lewat jalur bersarang {@code Audit_DeletedDate}. Garis bawah memberi tahu
 * Spring Data batas antara nama properti induk dan properti di dalamnya.
 */
public interface TrainingRepository extends JpaRepository<Training, Integer> {

    Optional<Training> findByIdAndAudit_DeletedDateIsNull(Integer id);

    Page<Training> findAllByAudit_DeletedDateIsNull(Pageable pageable);

    boolean existsByTemaIgnoreCaseAndAudit_DeletedDateIsNull(String tema);

    boolean existsByTemaIgnoreCaseAndAudit_DeletedDateIsNullAndIdNot(String tema, Integer id);
}
