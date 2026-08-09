package com.masesas.exercises.demo1.owasp;

import com.masesas.exercises.demo1.entity.DetailKaryawan;
import com.masesas.exercises.demo1.repository.DetailKaryawanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Penjaga kunci enkripsi terhadap data nyata.
 *
 * <p>Berbeda dari {@link A02CryptoTest} yang menanam datanya sendiri, kelas ini membaca
 * baris yang benar-benar ada di database. Kalau {@code app.crypto.key} diganti tanpa
 * mengenkripsi ulang, test ini gagal keras — jauh lebih baik daripada ketahuan
 * berbulan-bulan kemudian saat ada yang membuka slip gaji dan datanya sudah tidak
 * bisa dipulihkan.
 */
@SpringBootTest
class VerifikasiDekripsiTest {

    @Autowired
    private DetailKaryawanRepository detailKaryawanRepository;

    @Test
    @DisplayName("baris hasil migrasi terbaca kembali sebagai NIK dan NPWP yang wajar")
    void barisHasilMigrasiTerbacaKembali() {
        List<DetailKaryawan> sampel = detailKaryawanRepository.findAll();

        assertThat(sampel).isNotEmpty();
        assertThat(sampel).allSatisfy(detail -> {
            assertThat(detail.getNik()).matches("\\d{16}");
            assertThat(detail.getNpwp()).matches("\\d{15}");
        });
    }
}
