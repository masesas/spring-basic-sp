package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.PayrollRequest;
import com.masesas.exercises.demo1.dto.PayrollResponse;
import com.masesas.exercises.demo1.dto.PayrollUpdateRequest;
import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.entity.KomponenGaji;
import com.masesas.exercises.demo1.entity.PayrollId;
import com.masesas.exercises.demo1.entity.PayrollKaryawan;
import com.masesas.exercises.demo1.exception.BusinessRuleException;
import com.masesas.exercises.demo1.exception.DuplicateResourceException;
import com.masesas.exercises.demo1.exception.InvalidRequestException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.KaryawanRepository;
import com.masesas.exercises.demo1.repository.PayrollKaryawanRepository;
import com.masesas.exercises.demo1.service.impl.PayrollServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    private static final LocalDate PERIODE = LocalDate.of(2026, 8, 1);
    private static final Integer ID_KARYAWAN = 7;

    @Mock
    private PayrollKaryawanRepository payrollRepository;

    @Mock
    private KaryawanRepository karyawanRepository;

    private PayrollService payrollService;
    private Karyawan karyawan;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-07T00:00:00Z"), ZoneOffset.UTC);
        payrollService = new PayrollServiceImpl(payrollRepository, karyawanRepository, clock);

        karyawan = new Karyawan();
        karyawan.setId(ID_KARYAWAN);
        karyawan.setNama("Budi");
    }

    @Test
    @DisplayName("create menghitung gaji bersih dan bruto dari komponen")
    void create_menghitungBersihDanBruto() {
        when(karyawanRepository.findByIdAndDeletedDateIsNull(ID_KARYAWAN))
                .thenReturn(Optional.of(karyawan));
        when(payrollRepository.existsById_IdKaryawanAndId_Periode(ID_KARYAWAN, PERIODE))
                .thenReturn(false);
        when(payrollRepository.save(any(PayrollKaryawan.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PayrollResponse hasil = payrollService.create(new PayrollRequest(
                ID_KARYAWAN, PERIODE,
                new BigDecimal("8000000"), new BigDecimal("1500000"), new BigDecimal("500000")));

        assertThat(hasil.bruto()).isEqualByComparingTo("9500000");
        assertThat(hasil.bersih()).isEqualByComparingTo("9000000");
        assertThat(hasil.periode()).isEqualTo(PERIODE);
        assertThat(hasil.namaKaryawan()).isEqualTo("Budi");
    }

    @Test
    @DisplayName("periode dinormalkan ke tanggal 1 sehingga tanggal mana pun dalam bulan sama menunjuk baris yang sama")
    void create_menormalkanPeriodeKeAwalBulan() {
        when(karyawanRepository.findByIdAndDeletedDateIsNull(ID_KARYAWAN))
                .thenReturn(Optional.of(karyawan));
        when(payrollRepository.existsById_IdKaryawanAndId_Periode(ID_KARYAWAN, PERIODE))
                .thenReturn(false);
        when(payrollRepository.save(any(PayrollKaryawan.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PayrollResponse hasil = payrollService.create(new PayrollRequest(
                ID_KARYAWAN, LocalDate.of(2026, 8, 17),
                new BigDecimal("5000000"), BigDecimal.ZERO, BigDecimal.ZERO));

        assertThat(hasil.periode()).isEqualTo(PERIODE);
        verify(payrollRepository).existsById_IdKaryawanAndId_Periode(ID_KARYAWAN, PERIODE);
    }

    @Test
    @DisplayName("create menolak slip gaji kedua untuk karyawan dan periode yang sama")
    void create_menolakDuplikat() {
        when(karyawanRepository.findByIdAndDeletedDateIsNull(ID_KARYAWAN))
                .thenReturn(Optional.of(karyawan));
        when(payrollRepository.existsById_IdKaryawanAndId_Periode(ID_KARYAWAN, PERIODE))
                .thenReturn(true);

        assertThatThrownBy(() -> payrollService.create(new PayrollRequest(
                ID_KARYAWAN, PERIODE, new BigDecimal("5000000"), BigDecimal.ZERO, BigDecimal.ZERO)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(payrollRepository, never()).save(any());
    }

    @Test
    @DisplayName("create menolak potongan yang melebihi total penghasilan")
    void create_menolakBersihNegatif() {
        when(karyawanRepository.findByIdAndDeletedDateIsNull(ID_KARYAWAN))
                .thenReturn(Optional.of(karyawan));

        assertThatThrownBy(() -> payrollService.create(new PayrollRequest(
                ID_KARYAWAN, PERIODE,
                new BigDecimal("1000000"), BigDecimal.ZERO, new BigDecimal("2000000"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("potongan");
    }

    @Test
    @DisplayName("create menolak periode di masa depan")
    void create_menolakPeriodeMasaDepan() {
        when(karyawanRepository.findByIdAndDeletedDateIsNull(ID_KARYAWAN))
                .thenReturn(Optional.of(karyawan));

        assertThatThrownBy(() -> payrollService.create(new PayrollRequest(
                ID_KARYAWAN, LocalDate.of(2026, 12, 1),
                new BigDecimal("5000000"), BigDecimal.ZERO, BigDecimal.ZERO)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("update mencari baris memakai composite key yang utuh")
    void update_memakaiCompositeKey() {
        PayrollKaryawan existing = PayrollKaryawan.baru(
                karyawan, PERIODE, KomponenGaji.of(
                        new BigDecimal("8000000"), BigDecimal.ZERO, BigDecimal.ZERO),
                Instant.parse("2026-08-01T00:00:00Z"));

        when(payrollRepository.findById(new PayrollId(ID_KARYAWAN, PERIODE)))
                .thenReturn(Optional.of(existing));
        when(payrollRepository.save(any(PayrollKaryawan.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PayrollResponse hasil = payrollService.update(ID_KARYAWAN, PERIODE,
                new PayrollUpdateRequest(
                        new BigDecimal("9000000"), new BigDecimal("1000000"), new BigDecimal("250000")));

        assertThat(hasil.bersih()).isEqualByComparingTo("9750000");
        assertThat(hasil.updatedDate()).isEqualTo(Instant.parse("2026-08-07T00:00:00Z"));
    }

    @Test
    @DisplayName("findById melempar not found saat slip gaji tidak ada")
    void findById_tidakAda() {
        when(payrollRepository.findById(new PayrollId(ID_KARYAWAN, PERIODE)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> payrollService.findById(ID_KARYAWAN, PERIODE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("periode=2026-08-01");
    }

    @Test
    @DisplayName("PayrollId dengan nilai sama harus equal — syarat identitas Hibernate")
    void payrollId_equalsDanHashCode() {
        PayrollId a = new PayrollId(ID_KARYAWAN, PERIODE);
        PayrollId b = new PayrollId(ID_KARYAWAN, PERIODE);
        PayrollId c = new PayrollId(ID_KARYAWAN, PERIODE.plusMonths(1));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    @DisplayName("KomponenGaji memperlakukan null sebagai nol")
    void komponenGaji_nullDianggapNol() {
        KomponenGaji komponen = KomponenGaji.of(new BigDecimal("5000000"), null, null);

        assertThat(komponen.bruto()).isEqualByComparingTo("5000000");
        assertThat(komponen.bersih()).isEqualByComparingTo("5000000");
    }
}
