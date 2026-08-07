package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.CreateKaryawanRequest;
import com.masesas.exercises.demo1.dto.KaryawanResponse;
import com.masesas.exercises.demo1.dto.UpdateKaryawanRequest;
import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.repository.KaryawanRepository;
import com.masesas.exercises.demo1.repository.KaryawanTrainingRepository;
import com.masesas.exercises.demo1.repository.RekeningRepository;
import com.masesas.exercises.demo1.service.impl.KaryawanServiceImpl;
import com.masesas.exercises.demo1.service.support.DetailKaryawanWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Memastikan anotasi cache di KaryawanServiceImpl benar-benar bekerja.
 * Di test ini cache-nya in-memory (bukan Redis) supaya tidak perlu server hidup —
 * perilaku hit/miss/evict-nya sama persis dengan Redis.
 */
@SpringJUnitConfig(KaryawanServiceCacheTest.CacheTestConfig.class)
class KaryawanServiceCacheTest {

    private static final Integer ID = 1;
    private static final String CACHE_KARYAWAN = "karyawan";
    private static final String CACHE_ALL = "karyawan-all";

    @Autowired
    private KaryawanService karyawanService;

    @Autowired
    private KaryawanRepository karyawanRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        reset(karyawanRepository);
        cacheManager.getCache(CACHE_KARYAWAN).clear();
        cacheManager.getCache(CACHE_ALL).clear();

        when(karyawanRepository.findByIdAndDeletedDateIsNull(ID)).thenReturn(Optional.of(karyawan()));
    }

    @Test
    @DisplayName("findById dua kali -> database hanya dibaca sekali")
    void findById_dipanggilDuaKali_databaseHanyaSekali() {
        KaryawanResponse pertama = karyawanService.findById(ID);
        KaryawanResponse kedua = karyawanService.findById(ID);

        assertThat(pertama.nama()).isEqualTo("Budi");
        assertThat(kedua).isEqualTo(pertama);
        verify(karyawanRepository, times(1)).findByIdAndDeletedDateIsNull(ID);
    }

    @Test
    @DisplayName("findById menyimpan hasilnya ke cache 'karyawan'")
    void findById_menyimpanKeCache() {
        karyawanService.findById(ID);

        assertThat(cacheManager.getCache(CACHE_KARYAWAN).get(ID)).isNotNull();
    }

    @Test
    @DisplayName("findAll tanpa pagination dua kali -> database hanya dibaca sekali")
    void findAll_dipanggilDuaKali_databaseHanyaSekali() {
        when(karyawanRepository.findAllByDeletedDateIsNullOrderByIdAsc()).thenReturn(List.of(karyawan()));

        List<KaryawanResponse> pertama = karyawanService.findAll();
        List<KaryawanResponse> kedua = karyawanService.findAll();

        assertThat(pertama).hasSize(1);
        assertThat(kedua).isEqualTo(pertama);
        verify(karyawanRepository, times(1)).findAllByDeletedDateIsNullOrderByIdAsc();
        assertThat(cacheManager.getCache(CACHE_ALL).get("all")).isNotNull();
    }

    @Test
    @DisplayName("create menghapus cache daftar -> findAll berikutnya baca database lagi")
    void create_menghapusCacheDaftar() {
        when(karyawanRepository.findAllByDeletedDateIsNullOrderByIdAsc()).thenReturn(List.of(karyawan()));
        when(karyawanRepository.save(any(Karyawan.class))).thenReturn(karyawan());
        karyawanService.findAll();

        karyawanService.create(new CreateKaryawanRequest("Siti", "Bandung", LocalDate.of(1995, 5, 5), "AKTIF", null));

        assertThat(cacheManager.getCache(CACHE_ALL).get("all")).isNull();

        karyawanService.findAll();
        verify(karyawanRepository, times(2)).findAllByDeletedDateIsNullOrderByIdAsc();
    }

    @Test
    @DisplayName("update menghapus cache per-id dan cache daftar sekaligus")
    void update_menghapusKeduaCache() {
        when(karyawanRepository.findAllByDeletedDateIsNullOrderByIdAsc()).thenReturn(List.of(karyawan()));
        when(karyawanRepository.save(any(Karyawan.class))).thenReturn(karyawan());
        karyawanService.findById(ID);
        karyawanService.findAll();

        karyawanService.update(ID, new UpdateKaryawanRequest("Budi Baru", "Bandung", LocalDate.of(1990, 1, 1), "AKTIF"));

        assertThat(cacheManager.getCache(CACHE_KARYAWAN).get(ID)).isNull();
        assertThat(cacheManager.getCache(CACHE_ALL).get("all")).isNull();
    }

    @Test
    @DisplayName("update menghapus cache -> findById berikutnya baca database lagi")
    void update_menghapusCache() {
        karyawanService.findById(ID);
        when(karyawanRepository.save(any(Karyawan.class))).thenReturn(karyawan());

        karyawanService.update(ID, new UpdateKaryawanRequest("Budi Baru", "Bandung", LocalDate.of(1990, 1, 1), "AKTIF"));

        assertThat(cacheManager.getCache(CACHE_KARYAWAN).get(ID)).isNull();

        karyawanService.findById(ID);
        // 3 pembacaan: findById awal, update itu sendiri, dan findById setelah cache dihapus
        verify(karyawanRepository, times(3)).findByIdAndDeletedDateIsNull(ID);
    }

    private static Karyawan karyawan() {
        Karyawan karyawan = new Karyawan();
        karyawan.setId(ID);
        karyawan.setNama("Budi");
        karyawan.setAlamat("Jakarta");
        karyawan.setDob(LocalDate.of(1990, 1, 1));
        karyawan.setStatus("AKTIF");
        karyawan.setCreatedDate(Instant.parse("2024-01-01T00:00:00Z"));
        karyawan.setUpdatedDate(Instant.parse("2024-01-01T00:00:00Z"));
        return karyawan;
    }

    @Configuration
    @EnableCaching
    static class CacheTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CACHE_KARYAWAN, CACHE_ALL);
        }

        @Bean
        KaryawanRepository karyawanRepository() {
            return mock(KaryawanRepository.class);
        }

        @Bean
        RekeningRepository rekeningRepository() {
            return mock(RekeningRepository.class);
        }

        @Bean
        KaryawanTrainingRepository karyawanTrainingRepository() {
            return mock(KaryawanTrainingRepository.class);
        }

        @Bean
        DetailKaryawanWriter detailKaryawanWriter() {
            return mock(DetailKaryawanWriter.class);
        }

        @Bean
        JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
        }

        @Bean
        KaryawanService karyawanService(KaryawanRepository karyawanRepository,
                                        RekeningRepository rekeningRepository,
                                        KaryawanTrainingRepository karyawanTrainingRepository,
                                        DetailKaryawanWriter detailKaryawanWriter,
                                        JdbcTemplate jdbcTemplate) {
            return new KaryawanServiceImpl(karyawanRepository, rekeningRepository,
                    karyawanTrainingRepository, detailKaryawanWriter, jdbcTemplate);
        }
    }
}
