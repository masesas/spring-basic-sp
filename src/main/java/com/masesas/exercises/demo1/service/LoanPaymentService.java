package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.LoanPaymentRequest;
import com.masesas.exercises.demo1.dto.LoanPaymentResponse;
import com.masesas.exercises.demo1.entity.LoanApplication;
import com.masesas.exercises.demo1.entity.LoanPayment;
import com.masesas.exercises.demo1.entity.LoanPlafond;
import com.masesas.exercises.demo1.entity.StatusLoanApplication;
import com.masesas.exercises.demo1.exception.BusinessRuleException;
import com.masesas.exercises.demo1.exception.DuplicateResourceException;
import com.masesas.exercises.demo1.repository.LoanPaymentRepository;
import com.masesas.exercises.demo1.service.support.Validators;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanPaymentService {

    private final LoanPaymentRepository loanPaymentRepository;
    private final LoanApplicationService loanApplicationService;
    private final LoanPlafondService loanPlafondService;
    private final Clock clock;

    @Transactional
    public LoanPaymentResponse catat(LoanPaymentRequest request) {
        Validators.requireNotNull(request, "data pembayaran");
        LoanApplication pengajuan = loanApplicationService.requireExisting(request.getIdLoanApplication());

        if (pengajuan.getStatus() != StatusLoanApplication.DISBURSED) {
            throw new BusinessRuleException(
                    "Pembayaran hanya bisa dicatat untuk pinjaman berstatus DISBURSED, status saat ini "
                            + pengajuan.getStatus());
        }

        Integer angsuranKe = Validators.requireNotNull(request.getAngsuranKe(), "angsuran ke");
        loanPaymentRepository.findByLoanApplication_IdAndAngsuranKe(pengajuan.getId(), angsuranKe)
                .ifPresent(ada -> {
                    throw new DuplicateResourceException(
                            "Angsuran ke-" + angsuranKe + " untuk pinjaman ini sudah dicatat");
                });

        BigDecimal jumlah = Validators.requireNotNull(request.getJumlahBayar(), "jumlah bayar");
        BigDecimal totalSetelahIni = loanPaymentRepository.totalDibayar(pengajuan.getId()).add(jumlah);
        if (totalSetelahIni.compareTo(pengajuan.getJumlahPengajuan()) > 0) {
            throw new BusinessRuleException(
                    "Total pembayaran melebihi nilai pinjaman (" + pengajuan.getJumlahPengajuan() + ")");
        }

        Instant sekarang = Instant.now(clock);
        LoanPlafond plafond = loanPlafondService.requirePlafond(pengajuan.getCustomer().getId());
        plafond.kembalikan(jumlah, sekarang);

        LoanPayment payment = LoanPayment.baru(
                pengajuan,
                angsuranKe,
                jumlah,
                Validators.requireNotNull(request.getTanggalBayar(), "tanggal bayar"),
                Validators.requireText(request.getMetode(), "metode"),
                sekarang);

        return LoanPaymentResponse.from(loanPaymentRepository.save(payment));
    }

    public List<LoanPaymentResponse> daftarPerPengajuan(Integer idLoanApplication) {
        LoanApplication pengajuan = loanApplicationService.requireExisting(idLoanApplication);
        return loanPaymentRepository.findAllByLoanApplication_IdOrderByAngsuranKeAsc(pengajuan.getId())
                .stream()
                .map(LoanPaymentResponse::from)
                .toList();
    }

    public BigDecimal totalDibayar(Integer idLoanApplication) {
        LoanApplication pengajuan = loanApplicationService.requireExisting(idLoanApplication);
        return loanPaymentRepository.totalDibayar(pengajuan.getId());
    }
}
