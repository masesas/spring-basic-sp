package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.LoanProductRequest;
import com.masesas.exercises.demo1.dto.LoanProductResponse;
import com.masesas.exercises.demo1.entity.LoanProduct;
import com.masesas.exercises.demo1.entity.StatusLoanApplication;
import com.masesas.exercises.demo1.exception.BusinessRuleException;
import com.masesas.exercises.demo1.exception.DuplicateResourceException;
import com.masesas.exercises.demo1.exception.InvalidRequestException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.LoanApplicationRepository;
import com.masesas.exercises.demo1.repository.LoanProductRepository;
import com.masesas.exercises.demo1.service.support.Validators;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanProductService {

    private static final String RESOURCE = "LoanProduct";

    private static final List<StatusLoanApplication> STATUS_BERJALAN = List.of(
            StatusLoanApplication.DRAFT,
            StatusLoanApplication.SUBMITTED,
            StatusLoanApplication.APPROVED,
            StatusLoanApplication.DISBURSED);

    private final LoanProductRepository loanProductRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final Clock clock;

    @Transactional
    public LoanProductResponse create(LoanProductRequest request) {
        Validators.requireNotNull(request, "data produk pinjaman");
        String kode = kodeDari(request);

        if (loanProductRepository.existsByKodeIgnoreCaseAndDeletedDateIsNull(kode)) {
            throw new DuplicateResourceException("Produk pinjaman dengan kode tersebut sudah terdaftar");
        }

        Instant sekarang = Instant.now(clock);
        LoanProduct produk = new LoanProduct();
        produk.setKode(kode);
        produk.setCreatedDate(sekarang);
        terapkan(produk, request, sekarang);

        return LoanProductResponse.from(loanProductRepository.save(produk));
    }

    @Transactional
    public LoanProductResponse update(Integer id, LoanProductRequest request) {
        Validators.requireNotNull(request, "data produk pinjaman");
        LoanProduct produk = requireActive(id);
        String kode = kodeDari(request);

        if (loanProductRepository.existsByKodeIgnoreCaseAndDeletedDateIsNullAndIdNot(kode, id)) {
            throw new DuplicateResourceException("Produk pinjaman dengan kode tersebut sudah terdaftar");
        }

        produk.setKode(kode);
        terapkan(produk, request, Instant.now(clock));

        return LoanProductResponse.from(loanProductRepository.save(produk));
    }

    public LoanProductResponse findById(Integer id) {
        return LoanProductResponse.from(requireActive(id));
    }

    public Page<LoanProductResponse> findAll(Pageable pageable) {
        return loanProductRepository.findAllByDeletedDateIsNull(pageable).map(LoanProductResponse::from);
    }

    @Transactional
    public void delete(Integer id) {
        LoanProduct produk = requireActive(id);
        if (loanApplicationRepository.existsByLoanProduct_IdAndStatusIn(id, STATUS_BERJALAN)) {
            throw new BusinessRuleException(
                    "Produk masih dipakai pengajuan yang belum selesai, tidak bisa dihapus");
        }
        produk.setDeletedDate(Instant.now(clock));
        produk.setUpdatedDate(produk.getDeletedDate());
        loanProductRepository.save(produk);
    }

    LoanProduct requireActive(Integer id) {
        Validators.requireNotNull(id, "id produk pinjaman");
        return loanProductRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private void terapkan(LoanProduct produk, LoanProductRequest request, Instant timestamp) {
        Integer tenorMin = Validators.requireNotNull(request.getTenorMin(), "tenor minimum");
        Integer tenorMax = Validators.requireNotNull(request.getTenorMax(), "tenor maksimum");
        if (tenorMax < tenorMin) {
            throw new InvalidRequestException("tenor maksimum tidak boleh lebih kecil dari tenor minimum");
        }

        if (request.getPlafondMax().compareTo(request.getPlafondMin()) < 0) {
            throw new InvalidRequestException(
                    "plafond maksimum tidak boleh lebih kecil dari plafond minimum");
        }

        produk.setNama(Validators.requireText(request.getNama(), "nama"));
        produk.setBungaPersen(Validators.requireNotNull(request.getBungaPersen(), "bunga persen"));
        produk.setTenorMin(tenorMin);
        produk.setTenorMax(tenorMax);
        produk.setPlafondMin(request.getPlafondMin());
        produk.setPlafondMax(request.getPlafondMax());
        produk.setAktif(request.getAktif() == null || request.getAktif());
        produk.setUpdatedDate(timestamp);
    }

    private String kodeDari(LoanProductRequest request) {
        return Validators.requireText(request.getKode(), "kode").toUpperCase(Locale.ROOT);
    }
}
