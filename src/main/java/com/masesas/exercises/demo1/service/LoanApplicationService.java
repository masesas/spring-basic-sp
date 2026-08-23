package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.LoanApplicationRequest;
import com.masesas.exercises.demo1.dto.LoanApplicationResponse;
import com.masesas.exercises.demo1.entity.Branch;
import com.masesas.exercises.demo1.entity.Customer;
import com.masesas.exercises.demo1.entity.LoanApplication;
import com.masesas.exercises.demo1.entity.LoanPlafond;
import com.masesas.exercises.demo1.entity.LoanProduct;
import com.masesas.exercises.demo1.entity.StatusLoanApplication;
import com.masesas.exercises.demo1.exception.BusinessRuleException;
import com.masesas.exercises.demo1.exception.InvalidRequestException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.CustomerRepository;
import com.masesas.exercises.demo1.repository.LoanApplicationRepository;
import com.masesas.exercises.demo1.service.support.Validators;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanApplicationService {

    private static final String RESOURCE = "LoanApplication";

    private final LoanApplicationRepository loanApplicationRepository;
    private final CustomerRepository customerRepository;
    private final LoanProductService loanProductService;
    private final BranchService branchService;
    private final LoanPlafondService loanPlafondService;
    private final Clock clock;

    @Transactional
    public LoanApplicationResponse buatDraft(String emailCustomer, LoanApplicationRequest request) {
        Validators.requireNotNull(request, "data pengajuan");
        Customer customer = requireCustomer(emailCustomer);
        LoanProduct produk = requireProdukAktif(request.getIdLoanProduct());
        Branch branch = branchDari(request.getIdBranch());

        BigDecimal jumlah = Validators.requireNotNull(request.getJumlahPengajuan(), "jumlah pengajuan");
        Integer tenor = Validators.requireNotNull(request.getTenorBulan(), "tenor bulan");
        periksaTerhadapProduk(produk, jumlah, tenor);

        LoanApplication pengajuan = LoanApplication.baru(
                customer, produk, branch, jumlah, tenor, Instant.now(clock));

        return LoanApplicationResponse.from(loanApplicationRepository.save(pengajuan));
    }

    @Transactional
    public LoanApplicationResponse revisiDraft(
            String emailCustomer, Integer id, LoanApplicationRequest request) {
        Validators.requireNotNull(request, "data pengajuan");
        LoanApplication pengajuan = requireMilikCustomer(emailCustomer, id);
        LoanProduct produk = requireProdukAktif(request.getIdLoanProduct());
        Branch branch = branchDari(request.getIdBranch());

        BigDecimal jumlah = Validators.requireNotNull(request.getJumlahPengajuan(), "jumlah pengajuan");
        Integer tenor = Validators.requireNotNull(request.getTenorBulan(), "tenor bulan");
        periksaTerhadapProduk(produk, jumlah, tenor);

        pengajuan.revisi(produk, branch, jumlah, tenor, Instant.now(clock));

        return LoanApplicationResponse.from(loanApplicationRepository.saveAndFlush(pengajuan));
    }

    @Transactional
    public LoanApplicationResponse ajukan(String emailCustomer, Integer id) {
        LoanApplication pengajuan = requireMilikCustomer(emailCustomer, id);
        pengajuan.ajukan(Instant.now(clock));
        return LoanApplicationResponse.from(loanApplicationRepository.saveAndFlush(pengajuan));
    }

    @Transactional
    public LoanApplicationResponse batalkan(String emailCustomer, Integer id) {
        LoanApplication pengajuan = requireMilikCustomer(emailCustomer, id);
        pengajuan.batalkan(Instant.now(clock));
        return LoanApplicationResponse.from(loanApplicationRepository.saveAndFlush(pengajuan));
    }

    public Page<LoanApplicationResponse> daftarMilikCustomer(String emailCustomer, Pageable pageable) {
        Customer customer = requireCustomer(emailCustomer);
        return loanApplicationRepository.findAllByCustomer_Id(customer.getId(), pageable)
                .map(LoanApplicationResponse::from);
    }

    public LoanApplicationResponse detailMilikCustomer(String emailCustomer, Integer id) {
        return LoanApplicationResponse.from(requireMilikCustomer(emailCustomer, id));
    }

    public Page<LoanApplicationResponse> daftar(StatusLoanApplication status, Pageable pageable) {
        Page<LoanApplication> halaman = status == null
                ? loanApplicationRepository.findAllBy(pageable)
                : loanApplicationRepository.findAllByStatus(status, pageable);
        return halaman.map(LoanApplicationResponse::from);
    }

    public LoanApplicationResponse detail(Integer id) {
        return LoanApplicationResponse.from(requireExisting(id));
    }

    @Transactional
    public LoanApplicationResponse setujui(Integer id, String catatan) {
        LoanApplication pengajuan = requireExisting(id);
        LoanPlafond plafond = loanPlafondService.requirePlafond(pengajuan.getCustomer().getId());

        if (pengajuan.getJumlahPengajuan().compareTo(plafond.sisa()) > 0) {
            throw new BusinessRuleException(
                    "Jumlah pengajuan melebihi sisa plafond customer (sisa " + plafond.sisa() + ")");
        }

        pengajuan.setujui(Validators.trimOrNull(catatan), Instant.now(clock));
        return LoanApplicationResponse.from(loanApplicationRepository.saveAndFlush(pengajuan));
    }

    @Transactional
    public LoanApplicationResponse tolak(Integer id, String catatan) {
        LoanApplication pengajuan = requireExisting(id);
        String alasan = Validators.requireText(catatan, "catatan penolakan");
        pengajuan.tolak(alasan, Instant.now(clock));
        return LoanApplicationResponse.from(loanApplicationRepository.saveAndFlush(pengajuan));
    }

    @Transactional
    public LoanApplicationResponse cairkan(Integer id) {
        LoanApplication pengajuan = requireExisting(id);
        Instant sekarang = Instant.now(clock);

        LoanPlafond plafond = loanPlafondService.requirePlafond(pengajuan.getCustomer().getId());
        plafond.pakai(pengajuan.getJumlahPengajuan(), sekarang);

        pengajuan.cairkan(sekarang);
        return LoanApplicationResponse.from(loanApplicationRepository.saveAndFlush(pengajuan));
    }

    LoanApplication requireExisting(Integer id) {
        Validators.requireNotNull(id, "id pengajuan");
        return loanApplicationRepository.findWithRelasiById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private LoanApplication requireMilikCustomer(String emailCustomer, Integer id) {
        Customer customer = requireCustomer(emailCustomer);
        LoanApplication pengajuan = requireExisting(id);
        if (!pengajuan.dimilikiOleh(customer.getId())) {
            throw new ResourceNotFoundException(RESOURCE, id);
        }
        return pengajuan;
    }

    private Customer requireCustomer(String email) {
        return customerRepository.findByEmailAndDeletedDateIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", email));
    }

    private LoanProduct requireProdukAktif(Integer idLoanProduct) {
        LoanProduct produk = loanProductService.requireActive(idLoanProduct);
        if (produk.getAktif() == null || !produk.getAktif()) {
            throw new BusinessRuleException("Produk pinjaman sedang tidak aktif");
        }
        return produk;
    }

    private Branch branchDari(Integer idBranch) {
        return idBranch == null ? null : branchService.requireActive(idBranch);
    }

    private void periksaTerhadapProduk(LoanProduct produk, BigDecimal jumlah, Integer tenor) {
        if (jumlah.compareTo(produk.getPlafondMin()) < 0
                || jumlah.compareTo(produk.getPlafondMax()) > 0) {
            throw new InvalidRequestException("jumlah pengajuan harus antara "
                    + produk.getPlafondMin() + " dan " + produk.getPlafondMax());
        }
        if (tenor < produk.getTenorMin() || tenor > produk.getTenorMax()) {
            throw new InvalidRequestException("tenor harus antara "
                    + produk.getTenorMin() + " dan " + produk.getTenorMax() + " bulan");
        }
    }
}
