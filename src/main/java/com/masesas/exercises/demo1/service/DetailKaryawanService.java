package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.DetailKaryawanRequest;
import com.masesas.exercises.demo1.dto.DetailKaryawanResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DetailKaryawanService {

    DetailKaryawanResponse create(DetailKaryawanRequest request);

    DetailKaryawanResponse update(Integer id, DetailKaryawanRequest request);

    DetailKaryawanResponse findById(Integer id);

    Page<DetailKaryawanResponse> findAll(Pageable pageable);

    void delete(Integer id);
}
