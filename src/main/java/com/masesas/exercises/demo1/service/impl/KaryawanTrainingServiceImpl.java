package com.masesas.exercises.demo1.service.impl;

import com.masesas.exercises.demo1.dto.KaryawanTrainingRequest;
import com.masesas.exercises.demo1.dto.KaryawanTrainingResponse;
import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.entity.KaryawanTraining;
import com.masesas.exercises.demo1.entity.Training;
import com.masesas.exercises.demo1.exception.DuplicateResourceException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.KaryawanRepository;
import com.masesas.exercises.demo1.repository.KaryawanTrainingRepository;
import com.masesas.exercises.demo1.repository.TrainingRepository;
import com.masesas.exercises.demo1.service.KaryawanTrainingService;
import com.masesas.exercises.demo1.service.support.Validators;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class KaryawanTrainingServiceImpl implements KaryawanTrainingService {

    private static final String RESOURCE = "Karyawan training";

    private final KaryawanTrainingRepository karyawanTrainingRepository;
    private final KaryawanRepository karyawanRepository;
    private final TrainingRepository trainingRepository;
    private final Clock clock;

    public KaryawanTrainingServiceImpl(
            KaryawanTrainingRepository karyawanTrainingRepository,
            KaryawanRepository karyawanRepository,
            TrainingRepository trainingRepository,
            Clock clock) {
        this.karyawanTrainingRepository = karyawanTrainingRepository;
        this.karyawanRepository = karyawanRepository;
        this.trainingRepository = trainingRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public KaryawanTrainingResponse create(KaryawanTrainingRequest request) {
        Validators.requireNotNull(request, "data karyawan training");
        Karyawan karyawan = requireActiveKaryawan(request.idKaryawan());
        Training training = requireActiveTraining(request.idTraining());
        Validators.requireNotNull(request.tanggal(), "tanggal");

        if (karyawanTrainingRepository.existsByIdKaryawan_IdAndIdTraining_IdAndDeletedDateIsNull(
                karyawan.getId(), training.getId())) {
            throw new DuplicateResourceException("Karyawan sudah terdaftar pada training tersebut");
        }

        Instant now = Instant.now(clock);
        KaryawanTraining karyawanTraining = new KaryawanTraining();
        karyawanTraining.setIdKaryawan(karyawan);
        karyawanTraining.setIdTraining(training);
        karyawanTraining.setTanggal(request.tanggal());
        karyawanTraining.setCreatedDate(now);
        karyawanTraining.setUpdatedDate(now);

        return KaryawanTrainingResponse.from(karyawanTrainingRepository.save(karyawanTraining));
    }

    @Override
    @Transactional
    public KaryawanTrainingResponse update(Integer id, KaryawanTrainingRequest request) {
        Validators.requireNotNull(request, "data karyawan training");
        KaryawanTraining karyawanTraining = requireActive(id);
        Karyawan karyawan = requireActiveKaryawan(request.idKaryawan());
        Training training = requireActiveTraining(request.idTraining());
        Validators.requireNotNull(request.tanggal(), "tanggal");

        if (karyawanTrainingRepository.existsByIdKaryawan_IdAndIdTraining_IdAndDeletedDateIsNullAndIdNot(
                karyawan.getId(), training.getId(), id)) {
            throw new DuplicateResourceException("Karyawan sudah terdaftar pada training tersebut");
        }

        karyawanTraining.setIdKaryawan(karyawan);
        karyawanTraining.setIdTraining(training);
        karyawanTraining.setTanggal(request.tanggal());
        karyawanTraining.setUpdatedDate(Instant.now(clock));

        return KaryawanTrainingResponse.from(karyawanTrainingRepository.save(karyawanTraining));
    }

    @Override
    public KaryawanTrainingResponse findById(Integer id) {
        return KaryawanTrainingResponse.from(requireActive(id));
    }

    @Override
    public Page<KaryawanTrainingResponse> findAll(Pageable pageable) {
        return karyawanTrainingRepository.findAllByDeletedDateIsNull(pageable)
                .map(KaryawanTrainingResponse::from);
    }

    @Override
    public List<KaryawanTrainingResponse> findByKaryawan(Integer karyawanId) {
        requireActiveKaryawan(karyawanId);
        return karyawanTrainingRepository.findAllByIdKaryawan_IdAndDeletedDateIsNullOrderByTanggalDesc(karyawanId)
                .stream()
                .map(KaryawanTrainingResponse::from)
                .toList();
    }

    @Override
    public List<KaryawanTrainingResponse> findByTraining(Integer trainingId) {
        requireActiveTraining(trainingId);
        return karyawanTrainingRepository.findAllByIdTraining_IdAndDeletedDateIsNullOrderByTanggalDesc(trainingId)
                .stream()
                .map(KaryawanTrainingResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        KaryawanTraining karyawanTraining = requireActive(id);
        Instant now = Instant.now(clock);
        karyawanTraining.setDeletedDate(now);
        karyawanTraining.setUpdatedDate(now);
        karyawanTrainingRepository.save(karyawanTraining);
    }

    private KaryawanTraining requireActive(Integer id) {
        Validators.requireNotNull(id, "id karyawan training");
        return karyawanTrainingRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private Karyawan requireActiveKaryawan(Integer karyawanId) {
        Validators.requireNotNull(karyawanId, "id karyawan");
        return karyawanRepository.findByIdAndDeletedDateIsNull(karyawanId)
                .orElseThrow(() -> new ResourceNotFoundException("Karyawan", karyawanId));
    }

    private Training requireActiveTraining(Integer trainingId) {
        Validators.requireNotNull(trainingId, "id training");
        return trainingRepository.findByIdAndAudit_DeletedDateIsNull(trainingId)
                .orElseThrow(() -> new ResourceNotFoundException("Training", trainingId));
    }
}
