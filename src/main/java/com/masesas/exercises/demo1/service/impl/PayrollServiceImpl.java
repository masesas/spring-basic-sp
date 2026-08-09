package com.masesas.exercises.demo1.service.impl;

import com.masesas.exercises.demo1.dto.PayrollRequest;
import com.masesas.exercises.demo1.dto.PayrollResponse;
import com.masesas.exercises.demo1.dto.PayrollUpdateRequest;
import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.entity.KomponenGaji;
import com.masesas.exercises.demo1.entity.PayrollId;
import com.masesas.exercises.demo1.entity.PayrollKaryawan;
import com.masesas.exercises.demo1.exception.BusinessRuleException;
import com.masesas.exercises.demo1.exception.ConflictException;
import com.masesas.exercises.demo1.exception.DuplicateResourceException;
import com.masesas.exercises.demo1.exception.InvalidRequestException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.KaryawanRepository;
import com.masesas.exercises.demo1.repository.PayrollKaryawanRepository;
import com.masesas.exercises.demo1.service.PayrollService;
import com.masesas.exercises.demo1.service.support.Validators;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollServiceImpl implements PayrollService {

    private static final String RESOURCE = "Payroll";

    private final PayrollKaryawanRepository payrollRepository;
    private final KaryawanRepository karyawanRepository;
    private final Clock clock;

    @Override
    @Transactional
    public PayrollResponse create(PayrollRequest request) {
        Validators.requireNotNull(request, "data payroll");
        Karyawan karyawan = requireActiveKaryawan(request.idKaryawan());
        LocalDate periode = normalkanPeriode(request.periode());

        if (payrollRepository.existsById_IdKaryawanAndId_Periode(karyawan.getId(), periode)) {
            throw new DuplicateResourceException(
                    "Slip gaji periode " + periode + " untuk karyawan tersebut sudah ada");
        }

        KomponenGaji komponen = komponenDari(
                request.gajiPokok(), request.tunjangan(), request.potongan());

        PayrollKaryawan payroll =
                PayrollKaryawan.baru(karyawan, periode, komponen, Instant.now(clock));

        return PayrollResponse.from(payrollRepository.save(payroll));
    }

    @Override
    @Transactional
    public PayrollResponse update(Integer idKaryawan, LocalDate periode, PayrollUpdateRequest request) {
        Validators.requireNotNull(request, "data payroll");
        PayrollKaryawan payroll = requireExisting(idKaryawan, periode);
        requireVersiTerbaru(payroll, request.version());

        KomponenGaji komponen = komponenDari(
                request.gajiPokok(), request.tunjangan(), request.potongan());
        payroll.revisi(komponen, Instant.now(clock));

        // saveAndFlush, bukan save: @Version baru naik saat flush. Tanpa ini response
        // mengembalikan versi lama dan klien mengirimkannya kembali sebagai versi basi.
        return PayrollResponse.from(payrollRepository.saveAndFlush(payroll));
    }


    @Override
    @Transactional
    public PayrollResponse approve(Integer idKaryawan, LocalDate periode) {
        PayrollKaryawan payroll = requireExisting(idKaryawan, periode);
        payroll.setujui(Instant.now(clock));
        return PayrollResponse.from(payrollRepository.saveAndFlush(payroll));
    }

    @Override
    public PayrollResponse findById(Integer idKaryawan, LocalDate periode) {
        return PayrollResponse.from(requireExisting(idKaryawan, periode));
    }

    @Override
    public Page<PayrollResponse> findAll(Pageable pageable) {
        return payrollRepository.findAll(pageable).map(PayrollResponse::from);
    }

    @Override
    public Page<PayrollResponse> findByPeriode(LocalDate periode, Pageable pageable) {
        return payrollRepository.findAllById_Periode(normalkanPeriode(periode), pageable)
                .map(PayrollResponse::from);
    }

    @Override
    public List<PayrollResponse> findRiwayatKaryawan(Integer idKaryawan) {
        Karyawan karyawan = requireActiveKaryawan(idKaryawan);
        return payrollRepository.findAllById_IdKaryawanOrderById_PeriodeDesc(karyawan.getId())
                .stream()
                .map(PayrollResponse::from)
                .toList();
    }

    @Override
    public BigDecimal totalBersihPadaPeriode(LocalDate periode) {
        return payrollRepository.totalBersihPadaPeriode(normalkanPeriode(periode));
    }

    @Override
    @Transactional
    public void delete(Integer idKaryawan, LocalDate periode) {
        payrollRepository.delete(requireExisting(idKaryawan, periode));
    }

    /**
     * Periode selalu disimpan sebagai tanggal 1, sesuai CHECK constraint di database.
     * Menormalkan di sini membuat {@code 2026-08-01} dan {@code 2026-08-17} menunjuk
     * slip gaji yang sama, sehingga pemanggil tidak bisa membuat duplikat tanpa sengaja.
     */
    private LocalDate normalkanPeriode(LocalDate periode) {
        Validators.requireNotNull(periode, "periode");
        LocalDate awalBulan = periode.withDayOfMonth(1);
        if (awalBulan.isAfter(LocalDate.now(clock).withDayOfMonth(1))) {
            throw new InvalidRequestException("periode tidak boleh melebihi bulan berjalan");
        }
        return awalBulan;
    }

    private KomponenGaji komponenDari(BigDecimal gajiPokok, BigDecimal tunjangan, BigDecimal potongan) {
        KomponenGaji komponen = KomponenGaji.of(gajiPokok, tunjangan, potongan);

        if (komponen.getGajiPokok().signum() < 0
                || komponen.getTunjangan().signum() < 0
                || komponen.getPotongan().signum() < 0) {
            throw new InvalidRequestException("nominal gaji tidak boleh negatif");
        }
        if (komponen.bersih().signum() < 0) {
            throw new BusinessRuleException("potongan melebihi total penghasilan");
        }
        return komponen;
    }

    /**
     * {@code @Version} sendirian tidak cukup untuk API tanpa state: setiap request
     * memuat ulang barisnya, jadi Hibernate tidak pernah melihat konflik. Klien harus
     * mengirim balik versi yang dia lihat saat membaca — barulah edit di atas data
     * basi bisa dikenali.
     */
    private void requireVersiTerbaru(PayrollKaryawan payroll, Long versiDikirim) {
        if (versiDikirim != null && !versiDikirim.equals(payroll.getVersion())) {
            throw new ConflictException(
                    "Slip gaji sudah diubah orang lain (versi " + payroll.getVersion()
                            + ", Anda mengirim " + versiDikirim + "). Muat ulang lalu coba lagi.");
        }
    }

    private PayrollKaryawan requireExisting(Integer idKaryawan, LocalDate periode) {
        Validators.requireNotNull(idKaryawan, "id karyawan");
        LocalDate awalBulan = normalkanPeriode(periode);

        return payrollRepository.findById(new PayrollId(idKaryawan, awalBulan))
                .orElseThrow(() -> new ResourceNotFoundException(
                        RESOURCE, "karyawan=" + idKaryawan + ", periode=" + awalBulan));
    }

    private Karyawan requireActiveKaryawan(Integer idKaryawan) {
        Validators.requireNotNull(idKaryawan, "id karyawan");
        return karyawanRepository.findByIdAndDeletedDateIsNull(idKaryawan)
                .orElseThrow(() -> new ResourceNotFoundException("Karyawan", idKaryawan));
    }
}
