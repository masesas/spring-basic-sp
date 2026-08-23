package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.BranchRequest;
import com.masesas.exercises.demo1.dto.BranchResponse;
import com.masesas.exercises.demo1.entity.Branch;
import com.masesas.exercises.demo1.exception.BusinessRuleException;
import com.masesas.exercises.demo1.exception.DuplicateResourceException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.BranchRepository;
import com.masesas.exercises.demo1.repository.LoanApplicationRepository;
import com.masesas.exercises.demo1.service.support.Validators;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchService {

    private static final String RESOURCE = "Branch";

    private final BranchRepository branchRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final Clock clock;

    @Transactional
    public BranchResponse create(BranchRequest request) {
        Validators.requireNotNull(request, "data cabang");
        String kode = Validators.requireText(request.getKode(), "kode");
        String nama = Validators.requireText(request.getNama(), "nama");

        if (branchRepository.existsByKodeIgnoreCaseAndDeletedDateIsNull(kode)) {
            throw new DuplicateResourceException("Cabang dengan kode tersebut sudah terdaftar");
        }

        Instant sekarang = Instant.now(clock);
        Branch branch = new Branch();
        branch.setKode(kode);
        branch.setNama(nama);
        branch.setAlamat(Validators.trimOrNull(request.getAlamat()));
        branch.setCreatedDate(sekarang);
        branch.setUpdatedDate(sekarang);

        return BranchResponse.from(branchRepository.save(branch));
    }

    @Transactional
    public BranchResponse update(Integer id, BranchRequest request) {
        Validators.requireNotNull(request, "data cabang");
        Branch branch = requireActive(id);
        String kode = Validators.requireText(request.getKode(), "kode");
        String nama = Validators.requireText(request.getNama(), "nama");

        if (branchRepository.existsByKodeIgnoreCaseAndDeletedDateIsNullAndIdNot(kode, id)) {
            throw new DuplicateResourceException("Cabang dengan kode tersebut sudah terdaftar");
        }

        branch.setKode(kode);
        branch.setNama(nama);
        branch.setAlamat(Validators.trimOrNull(request.getAlamat()));
        branch.setUpdatedDate(Instant.now(clock));

        return BranchResponse.from(branchRepository.save(branch));
    }

    public BranchResponse findById(Integer id) {
        return BranchResponse.from(requireActive(id));
    }

    public Page<BranchResponse> findAll(Pageable pageable) {
        return branchRepository.findAllByDeletedDateIsNull(pageable).map(BranchResponse::from);
    }

    @Transactional
    public void delete(Integer id) {
        Branch branch = requireActive(id);
        if (loanApplicationRepository.existsByBranch_Id(id)) {
            throw new BusinessRuleException(
                    "Cabang masih dipakai pengajuan pinjaman, tidak bisa dihapus");
        }
        branch.setDeletedDate(Instant.now(clock));
        branch.setUpdatedDate(branch.getDeletedDate());
        branchRepository.save(branch);
    }

    Branch requireActive(Integer id) {
        Validators.requireNotNull(id, "id cabang");
        return branchRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }
}
