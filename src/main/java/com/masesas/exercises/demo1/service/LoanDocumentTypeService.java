package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.LoanDocumentTypeRequest;
import com.masesas.exercises.demo1.dto.LoanDocumentTypeResponse;
import com.masesas.exercises.demo1.entity.LoanDocumentType;
import com.masesas.exercises.demo1.exception.DuplicateResourceException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.LoanDocumentTypeRepository;
import com.masesas.exercises.demo1.service.support.Validators;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanDocumentTypeService {

    private static final String RESOURCE = "LoanDocumentType";

    private final LoanDocumentTypeRepository loanDocumentTypeRepository;
    private final Clock clock;

    @Transactional
    public LoanDocumentTypeResponse create(LoanDocumentTypeRequest request) {
        Validators.requireNotNull(request, "data jenis dokumen");
        String kode = kodeDari(request);

        if (loanDocumentTypeRepository.existsByKodeIgnoreCaseAndDeletedDateIsNull(kode)) {
            throw new DuplicateResourceException("Jenis dokumen dengan kode tersebut sudah terdaftar");
        }

        Instant sekarang = Instant.now(clock);
        LoanDocumentType jenis = new LoanDocumentType();
        jenis.setKode(kode);
        jenis.setNama(Validators.requireText(request.getNama(), "nama"));
        jenis.setWajib(request.getWajib() != null && request.getWajib());
        jenis.setCreatedDate(sekarang);
        jenis.setUpdatedDate(sekarang);

        return LoanDocumentTypeResponse.from(loanDocumentTypeRepository.save(jenis));
    }

    @Transactional
    public LoanDocumentTypeResponse update(Integer id, LoanDocumentTypeRequest request) {
        Validators.requireNotNull(request, "data jenis dokumen");
        LoanDocumentType jenis = requireActive(id);
        String kode = kodeDari(request);

        if (loanDocumentTypeRepository.existsByKodeIgnoreCaseAndDeletedDateIsNullAndIdNot(kode, id)) {
            throw new DuplicateResourceException("Jenis dokumen dengan kode tersebut sudah terdaftar");
        }

        jenis.setKode(kode);
        jenis.setNama(Validators.requireText(request.getNama(), "nama"));
        jenis.setWajib(request.getWajib() != null && request.getWajib());
        jenis.setUpdatedDate(Instant.now(clock));

        return LoanDocumentTypeResponse.from(loanDocumentTypeRepository.save(jenis));
    }

    public LoanDocumentTypeResponse findById(Integer id) {
        return LoanDocumentTypeResponse.from(requireActive(id));
    }

    public Page<LoanDocumentTypeResponse> findAll(Pageable pageable) {
        return loanDocumentTypeRepository.findAllByDeletedDateIsNull(pageable)
                .map(LoanDocumentTypeResponse::from);
    }

    @Transactional
    public void delete(Integer id) {
        LoanDocumentType jenis = requireActive(id);
        jenis.setDeletedDate(Instant.now(clock));
        jenis.setUpdatedDate(jenis.getDeletedDate());
        loanDocumentTypeRepository.save(jenis);
    }

    private LoanDocumentType requireActive(Integer id) {
        Validators.requireNotNull(id, "id jenis dokumen");
        return loanDocumentTypeRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private String kodeDari(LoanDocumentTypeRequest request) {
        return Validators.requireText(request.getKode(), "kode").toUpperCase(Locale.ROOT);
    }
}
