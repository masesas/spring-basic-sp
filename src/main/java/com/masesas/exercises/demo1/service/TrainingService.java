package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.TrainingRequest;
import com.masesas.exercises.demo1.dto.TrainingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TrainingService {

    TrainingResponse create(TrainingRequest request);

    TrainingResponse update(Integer id, TrainingRequest request);

    TrainingResponse findById(Integer id);

    Page<TrainingResponse> findAll(Pageable pageable);

    void delete(Integer id);
}
