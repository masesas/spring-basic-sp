package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.LoanPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface LoanPaymentRepository extends JpaRepository<LoanPayment, Integer> {

    List<LoanPayment> findAllByLoanApplication_IdOrderByAngsuranKeAsc(Integer idLoanApplication);

    Optional<LoanPayment> findByLoanApplication_IdAndAngsuranKe(Integer idLoanApplication, Integer angsuranKe);

    @Query("select coalesce(sum(p.jumlahBayar), 0) from LoanPayment p "
            + "where p.loanApplication.id = :idLoanApplication")
    BigDecimal totalDibayar(@Param("idLoanApplication") Integer idLoanApplication);
}
