package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.PayrollRequest;
import com.masesas.exercises.demo1.dto.PayrollResponse;
import com.masesas.exercises.demo1.dto.PayrollUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PayrollService {

    PayrollResponse create(PayrollRequest request);

    PayrollResponse update(Integer idKaryawan, LocalDate periode, PayrollUpdateRequest request);

    PayrollResponse approve(Integer idKaryawan, LocalDate periode);

    PayrollResponse findById(Integer idKaryawan, LocalDate periode);

    Page<PayrollResponse> findAll(Pageable pageable);

    Page<PayrollResponse> findByPeriode(LocalDate periode, Pageable pageable);

    List<PayrollResponse> findRiwayatKaryawan(Integer idKaryawan);

    BigDecimal totalBersihPadaPeriode(LocalDate periode);

    void delete(Integer idKaryawan, LocalDate periode);
}
