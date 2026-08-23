package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.LoanPlafond;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanPlafondRepository extends JpaRepository<LoanPlafond, Integer> {

    @EntityGraph(attributePaths = "customer")
    Page<LoanPlafond> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = "customer")
    Optional<LoanPlafond> findByCustomer_Id(Integer idCustomer);
}
