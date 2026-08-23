package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Integer> {

    Page<Branch> findAllByDeletedDateIsNull(Pageable pageable);

    Optional<Branch> findByIdAndDeletedDateIsNull(Integer id);

    boolean existsByKodeIgnoreCaseAndDeletedDateIsNull(String kode);

    boolean existsByKodeIgnoreCaseAndDeletedDateIsNullAndIdNot(String kode, Integer id);
}
