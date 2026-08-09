package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.PayrollId;
import com.masesas.exercises.demo1.entity.PayrollKaryawan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PayrollKaryawanRepository extends JpaRepository<PayrollKaryawan, PayrollId> {

    boolean existsById_IdKaryawanAndId_Periode(Integer idKaryawan, LocalDate periode);

    Page<PayrollKaryawan> findAllById_Periode(LocalDate periode, Pageable pageable);

    List<PayrollKaryawan> findAllById_IdKaryawanOrderById_PeriodeDesc(Integer idKaryawan);

    @Query("""
            select coalesce(sum(p.komponen.gajiPokok + p.komponen.tunjangan - p.komponen.potongan), 0)
            from PayrollKaryawan p
            where p.id.periode = :periode
            """)
    BigDecimal totalBersihPadaPeriode(@Param("periode") LocalDate periode);
}
