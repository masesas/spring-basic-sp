package com.masesas.exercises.demo1.service.impl;

import com.masesas.exercises.demo1.dto.DetailKaryawanRequest;
import com.masesas.exercises.demo1.dto.DetailKaryawanResponse;
import com.masesas.exercises.demo1.entity.DetailKaryawan;
import com.masesas.exercises.demo1.exception.BusinessRuleException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.DetailKaryawanRepository;
import com.masesas.exercises.demo1.repository.KaryawanRepository;
import com.masesas.exercises.demo1.service.DetailKaryawanService;
import com.masesas.exercises.demo1.service.support.DetailKaryawanWriter;
import com.masesas.exercises.demo1.service.support.Validators;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class DetailKaryawanServiceImpl implements DetailKaryawanService {

    private static final String RESOURCE = "Detail karyawan";

    private final DetailKaryawanRepository detailKaryawanRepository;
    private final KaryawanRepository karyawanRepository;
    private final DetailKaryawanWriter detailKaryawanWriter;
    private final Clock clock;

    public DetailKaryawanServiceImpl(
            DetailKaryawanRepository detailKaryawanRepository,
            KaryawanRepository karyawanRepository,
            DetailKaryawanWriter detailKaryawanWriter,
            Clock clock) {
        this.detailKaryawanRepository = detailKaryawanRepository;
        this.karyawanRepository = karyawanRepository;
        this.detailKaryawanWriter = detailKaryawanWriter;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DetailKaryawanResponse create(DetailKaryawanRequest request) {
        return DetailKaryawanResponse.from(detailKaryawanWriter.create(request, Instant.now(clock)));
    }

    @Override
    @Transactional
    public DetailKaryawanResponse update(Integer id, DetailKaryawanRequest request) {
        DetailKaryawan detail = requireActive(id);
        return DetailKaryawanResponse.from(detailKaryawanWriter.update(detail, request, Instant.now(clock)));
    }

    @Override
    public DetailKaryawanResponse findById(Integer id) {
        return DetailKaryawanResponse.from(requireActive(id));
    }

    @Override
    public Page<DetailKaryawanResponse> findAll(Pageable pageable) {
        return detailKaryawanRepository.findAllByDeletedDateIsNull(pageable).map(DetailKaryawanResponse::from);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        DetailKaryawan detail = requireActive(id);

        if (karyawanRepository.existsByDetailKaryawan_IdAndDeletedDateIsNull(id)) {
            throw new BusinessRuleException(
                    "Detail karyawan masih dipakai oleh karyawan aktif, lepaskan dari karyawan terlebih dahulu");
        }

        detailKaryawanWriter.softDelete(detail, Instant.now(clock));
    }

    private DetailKaryawan requireActive(Integer id) {
        Validators.requireNotNull(id, "id detail karyawan");
        return detailKaryawanRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }
}
