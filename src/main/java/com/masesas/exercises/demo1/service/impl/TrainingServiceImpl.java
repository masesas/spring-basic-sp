package com.masesas.exercises.demo1.service.impl;

import com.masesas.exercises.demo1.dto.TrainingRequest;
import com.masesas.exercises.demo1.dto.TrainingResponse;
import com.masesas.exercises.demo1.entity.AuditDates;
import com.masesas.exercises.demo1.entity.Training;
import com.masesas.exercises.demo1.exception.DuplicateResourceException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.KaryawanTrainingRepository;
import com.masesas.exercises.demo1.repository.TrainingRepository;
import com.masesas.exercises.demo1.service.TrainingService;
import com.masesas.exercises.demo1.service.support.Validators;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class TrainingServiceImpl implements TrainingService {

    private static final String RESOURCE = "Training";

    private final TrainingRepository trainingRepository;
    private final KaryawanTrainingRepository karyawanTrainingRepository;
    private final Clock clock;

    public TrainingServiceImpl(
            TrainingRepository trainingRepository,
            KaryawanTrainingRepository karyawanTrainingRepository,
            Clock clock) {
        this.trainingRepository = trainingRepository;
        this.karyawanTrainingRepository = karyawanTrainingRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TrainingResponse create(TrainingRequest request) {
        Validators.requireNotNull(request, "data training");
        String tema = Validators.requireText(request.getTema(), "tema");
        String pengajar = Validators.requireText(request.getPengajar(), "pengajar");

        if (trainingRepository.existsByTemaIgnoreCaseAndAudit_DeletedDateIsNull(tema)) {
            throw new DuplicateResourceException("Training dengan tema tersebut sudah terdaftar");
        }

        Instant now = Instant.now(clock);
        Training training = new Training();
        training.setTema(tema);
        training.setPengajar(pengajar);
        training.setAudit(AuditDates.createdAt(now));

        return TrainingResponse.from(trainingRepository.save(training));
    }

    @Override
    @Transactional
    public TrainingResponse update(Integer id, TrainingRequest request) {
        Validators.requireNotNull(request, "data training");
        Training training = requireActive(id);
        String tema = Validators.requireText(request.getTema(), "tema");
        String pengajar = Validators.requireText(request.getPengajar(), "pengajar");

        if (trainingRepository.existsByTemaIgnoreCaseAndAudit_DeletedDateIsNullAndIdNot(tema, id)) {
            throw new DuplicateResourceException("Training dengan tema tersebut sudah terdaftar");
        }

        training.setTema(tema);
        training.setPengajar(pengajar);
        training.setAudit(training.getAudit().touched(Instant.now(clock)));

        return TrainingResponse.from(trainingRepository.save(training));
    }

    @Override
    public TrainingResponse findById(Integer id) {
        return TrainingResponse.from(requireActive(id));
    }

    @Override
    public Page<TrainingResponse> findAll(Pageable pageable) {
        return trainingRepository.findAllByAudit_DeletedDateIsNull(pageable).map(TrainingResponse::from);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Training training = requireActive(id);
        Instant now = Instant.now(clock);

        training.setAudit(training.getAudit().deletedAt(now));
        trainingRepository.save(training);

        karyawanTrainingRepository.softDeleteByTrainingId(id, now);
    }

    private Training requireActive(Integer id) {
        Validators.requireNotNull(id, "id training");
        return trainingRepository.findByIdAndAudit_DeletedDateIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }
}
