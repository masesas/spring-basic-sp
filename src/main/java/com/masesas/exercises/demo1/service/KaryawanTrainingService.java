package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.KaryawanTrainingRequest;
import com.masesas.exercises.demo1.dto.KaryawanTrainingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface KaryawanTrainingService {

    KaryawanTrainingResponse create(KaryawanTrainingRequest request);

    KaryawanTrainingResponse update(Integer id, KaryawanTrainingRequest request);

    KaryawanTrainingResponse findById(Integer id);

    Page<KaryawanTrainingResponse> findAll(Pageable pageable);

    List<KaryawanTrainingResponse> findByKaryawan(Integer karyawanId);

    List<KaryawanTrainingResponse> findByTraining(Integer trainingId);

    void delete(Integer id);
}
