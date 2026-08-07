package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.CreateKaryawanRequest;
import com.masesas.exercises.demo1.dto.DetailKaryawanRequest;
import com.masesas.exercises.demo1.dto.KaryawanResponse;
import com.masesas.exercises.demo1.dto.UpdateKaryawanRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface KaryawanService {

    KaryawanResponse create(CreateKaryawanRequest request);

    KaryawanResponse update(Integer id, UpdateKaryawanRequest request);

    KaryawanResponse findById(Integer id);

    Page<KaryawanResponse> findAll(Pageable pageable);

    Page<KaryawanResponse> findPage(int page, int size);

    Page<KaryawanResponse> findPageByNama(String nama, int page, int size);

    /** Semua karyawan aktif tanpa pagination — hasilnya di-cache. */
    List<KaryawanResponse> findAll();

    void delete(Integer id);

    KaryawanResponse upsertDetail(Integer karyawanId, DetailKaryawanRequest request);

    KaryawanResponse removeDetail(Integer karyawanId);
}
