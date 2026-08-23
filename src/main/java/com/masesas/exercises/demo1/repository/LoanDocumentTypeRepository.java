package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.LoanDocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanDocumentTypeRepository extends JpaRepository<LoanDocumentType, Integer> {

    Page<LoanDocumentType> findAllByDeletedDateIsNull(Pageable pageable);

    Optional<LoanDocumentType> findByIdAndDeletedDateIsNull(Integer id);

    boolean existsByKodeIgnoreCaseAndDeletedDateIsNull(String kode);

    boolean existsByKodeIgnoreCaseAndDeletedDateIsNullAndIdNot(String kode, Integer id);
}
