package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.RekeningRequest;
import com.masesas.exercises.demo1.dto.RekeningResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RekeningService {

    RekeningResponse create(RekeningRequest request);

    RekeningResponse update(Integer id, RekeningRequest request);

    RekeningResponse findById(Integer id);

    Page<RekeningResponse> findAll(Pageable pageable);

    List<RekeningResponse> findByKaryawan(Integer karyawanId);

    void delete(Integer id);
}
