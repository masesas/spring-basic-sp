package com.masesas.exercises.demo1.service.impl;

import com.masesas.exercises.demo1.dto.RekeningRequest;
import com.masesas.exercises.demo1.dto.RekeningResponse;
import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.entity.Rekening;
import com.masesas.exercises.demo1.exception.DuplicateResourceException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.KaryawanRepository;
import com.masesas.exercises.demo1.repository.RekeningRepository;
import com.masesas.exercises.demo1.service.RekeningService;
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
public class RekeningServiceImpl implements RekeningService {

    private static final String RESOURCE = "Rekening";

    private final RekeningRepository rekeningRepository;
    private final KaryawanRepository karyawanRepository;
    private final Clock clock;

    public RekeningServiceImpl(
            RekeningRepository rekeningRepository,
            KaryawanRepository karyawanRepository,
            Clock clock) {
        this.rekeningRepository = rekeningRepository;
        this.karyawanRepository = karyawanRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RekeningResponse create(RekeningRequest request) {
        Validators.requireNotNull(request, "data rekening");
        String jenis = Validators.requireText(request.jenis(), "jenis");
        String nama = Validators.requireText(request.nama(), "nama pemilik rekening");
        String nomor = Validators.requireText(request.rekening(), "nomor rekening");
        Karyawan karyawan = requireActiveKaryawan(request.idKaryawan());

        if (rekeningRepository.existsByJenisIgnoreCaseAndRekeningAndDeletedDateIsNull(jenis, nomor)) {
            throw new DuplicateResourceException("Nomor rekening sudah terdaftar untuk jenis " + jenis);
        }

        Instant now = Instant.now(clock);
        Rekening rekening = new Rekening();
        rekening.setIdKaryawan(karyawan);
        rekening.setJenis(jenis);
        rekening.setNama(nama);
        rekening.setRekening(nomor);
        rekening.setCreatedDate(now);
        rekening.setUpdatedDate(now);

        return RekeningResponse.from(rekeningRepository.save(rekening));
    }

    @Override
    @Transactional
    public RekeningResponse update(Integer id, RekeningRequest request) {
        Validators.requireNotNull(request, "data rekening");
        Rekening rekening = requireActive(id);
        String jenis = Validators.requireText(request.jenis(), "jenis");
        String nama = Validators.requireText(request.nama(), "nama pemilik rekening");
        String nomor = Validators.requireText(request.rekening(), "nomor rekening");
        Karyawan karyawan = requireActiveKaryawan(request.idKaryawan());

        if (rekeningRepository.existsByJenisIgnoreCaseAndRekeningAndDeletedDateIsNullAndIdNot(jenis, nomor, id)) {
            throw new DuplicateResourceException("Nomor rekening sudah terdaftar untuk jenis " + jenis);
        }

        rekening.setIdKaryawan(karyawan);
        rekening.setJenis(jenis);
        rekening.setNama(nama);
        rekening.setRekening(nomor);
        rekening.setUpdatedDate(Instant.now(clock));

        return RekeningResponse.from(rekeningRepository.save(rekening));
    }

    @Override
    public RekeningResponse findById(Integer id) {
        return RekeningResponse.from(requireActive(id));
    }

    @Override
    public Page<RekeningResponse> findAll(Pageable pageable) {
        return rekeningRepository.findAllByDeletedDateIsNull(pageable).map(RekeningResponse::from);
    }

    @Override
    public List<RekeningResponse> findByKaryawan(Integer karyawanId) {
        requireActiveKaryawan(karyawanId);
        return rekeningRepository.findAllByIdKaryawan_IdAndDeletedDateIsNullOrderByIdAsc(karyawanId)
                .stream()
                .map(RekeningResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Rekening rekening = requireActive(id);
        Instant now = Instant.now(clock);
        rekening.setDeletedDate(now);
        rekening.setUpdatedDate(now);
        rekeningRepository.save(rekening);
    }

    private Rekening requireActive(Integer id) {
        Validators.requireNotNull(id, "id rekening");
        return rekeningRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private Karyawan requireActiveKaryawan(Integer karyawanId) {
        Validators.requireNotNull(karyawanId, "id karyawan");
        return karyawanRepository.findByIdAndDeletedDateIsNull(karyawanId)
                .orElseThrow(() -> new ResourceNotFoundException("Karyawan", karyawanId));
    }
}
