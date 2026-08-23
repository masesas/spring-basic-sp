package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.LoanApplication;
import com.masesas.exercises.demo1.entity.StatusLoanApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Integer> {

    @EntityGraph(attributePaths = {"customer", "loanProduct", "branch"})
    Page<LoanApplication> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "loanProduct", "branch"})
    Page<LoanApplication> findAllByStatus(StatusLoanApplication status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "loanProduct", "branch"})
    Page<LoanApplication> findAllByCustomer_Id(Integer idCustomer, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "loanProduct", "branch"})
    Optional<LoanApplication> findWithRelasiById(Integer id);

    boolean existsByLoanProduct_IdAndStatusIn(Integer idLoanProduct, Iterable<StatusLoanApplication> status);

    boolean existsByBranch_Id(Integer idBranch);
}
