package com.masesas.exercises.demo1.security;

import com.masesas.exercises.demo1.entity.DetailKaryawan;
import com.masesas.exercises.demo1.repository.DetailKaryawanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CryptoConverterTest {

    private static final String PENANDA = "enc:v1:";

    @Autowired
    private CryptoConverter cryptoConverter;

    @Autowired
    private DetailKaryawanRepository detailKaryawanRepository;

    @Test
    @DisplayName("nilai yang dienkripsi bisa dikembalikan utuh")
    void bolakBalik() {
        String asli = "3201234567890001";

        String tersimpan = cryptoConverter.convertToDatabaseColumn(asli);

        assertThat(tersimpan).startsWith(PENANDA).isNotEqualTo(asli);
        assertThat(cryptoConverter.convertToEntityAttribute(tersimpan)).isEqualTo(asli);
    }

    @Test
    @DisplayName("dua enkripsi atas nilai sama menghasilkan ciphertext berbeda")
    void ivAcakPerEnkripsi() {
        String asli = "3201234567890001";

        assertThat(cryptoConverter.convertToDatabaseColumn(asli))
                .isNotEqualTo(cryptoConverter.convertToDatabaseColumn(asli));
    }

    @Test
    @DisplayName("nilai lama tanpa penanda dikembalikan apa adanya")
    void teksBiasaDilewatkan() {
        assertThat(cryptoConverter.convertToEntityAttribute("3201234567890001"))
                .isEqualTo("3201234567890001");
        assertThat(cryptoConverter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("baris yang sudah terenkripsi di database terbaca sebagai teks biasa")
    void barisDatabaseTerdekripsi() {
        DetailKaryawan detail = detailKaryawanRepository
                .findAllByDeletedDateIsNull(PageRequest.of(0, 1))
                .getContent().stream()
                .findFirst()
                .orElseThrow();

        assertThat(detail.getNik()).doesNotStartWith(PENANDA);
        assertThat(detail.getNpwp()).doesNotStartWith(PENANDA);
    }
}
