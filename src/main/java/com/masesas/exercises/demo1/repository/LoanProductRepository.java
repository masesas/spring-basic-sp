package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.LoanProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Integer> {

    Page<LoanProduct> findAllByDeletedDateIsNull(Pageable pageable);

    Optional<LoanProduct> findByIdAndDeletedDateIsNull(Integer id);

    boolean existsByKodeIgnoreCaseAndDeletedDateIsNull(String kode);

    boolean existsByKodeIgnoreCaseAndDeletedDateIsNullAndIdNot(String kode, Integer id);
}
