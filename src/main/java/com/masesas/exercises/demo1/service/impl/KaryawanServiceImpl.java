package com.masesas.exercises.demo1.service.impl;

import com.masesas.exercises.demo1.dto.CreateKaryawanRequest;
import com.masesas.exercises.demo1.dto.DetailKaryawanRequest;
import com.masesas.exercises.demo1.dto.KaryawanResponse;
import com.masesas.exercises.demo1.dto.UpdateKaryawanRequest;
import com.masesas.exercises.demo1.entity.DetailKaryawan;
import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.KaryawanRepository;
import com.masesas.exercises.demo1.repository.KaryawanTrainingRepository;
import com.masesas.exercises.demo1.repository.RekeningRepository;
import com.masesas.exercises.demo1.service.KaryawanService;
import com.masesas.exercises.demo1.service.support.DetailKaryawanWriter;
import com.masesas.exercises.demo1.service.support.Validators;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KaryawanServiceImpl implements KaryawanService {

    private static final String RESOURCE = "Karyawan";

    /**
     * Nama cache di Redis. Key akhir jadi "demo1:karyawan::{id}" (prefix dari RedisConfig).
     */
    private static final String CACHE_KARYAWAN = "karyawan";

    /**
     * Cache terpisah untuk daftar semua karyawan. Key akhir: "demo1:karyawan-all::all".
     */
    private static final String CACHE_KARYAWAN_ALL = "karyawan-all";

    private final KaryawanRepository karyawanRepository;
    private final RekeningRepository rekeningRepository;
    private final KaryawanTrainingRepository karyawanTrainingRepository;
    private final DetailKaryawanWriter detailKaryawanWriter;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Ada karyawan baru -> daftar lama sudah tidak lengkap, buang dari cache.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_KARYAWAN_ALL, key = "'all'")
    public KaryawanResponse create(CreateKaryawanRequest request) {
        Validators.requireNotNull(request, "data karyawan");
        Instant now = Instant.now();

        Karyawan karyawan = new Karyawan();
        karyawan.setNama(Validators.requireText(request.getNama(), "nama"));
        karyawan.setAlamat(Validators.trimOrNull(request.getAlamat()));
        karyawan.setDob(Validators.requireNotFuture(request.getDob(), "dob", LocalDate.now()));
        karyawan.setStatus(Validators.requireText(request.getStatus(), "status"));
        karyawan.setCreatedDate(now);
        karyawan.setUpdatedDate(now);

        if (request.getDetail() != null) {
            karyawan.setDetailKaryawan(detailKaryawanWriter.create(request.getDetail(), now));
        }

        return KaryawanResponse.from(karyawanRepository.save(karyawan));
    }

    /**
     * Data berubah -> buang cache lama supaya pembacaan berikutnya ambil dari database.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_KARYAWAN, key = "#id"),
            @CacheEvict(cacheNames = CACHE_KARYAWAN_ALL, key = "'all'")
    })
    public KaryawanResponse update(Integer id, UpdateKaryawanRequest request) {
        Validators.requireNotNull(request, "data karyawan");
        Karyawan karyawan = requireActive(id);
        Instant now = Instant.now();

        karyawan.setNama(Validators.requireText(request.getNama(), "nama"));
        karyawan.setAlamat(Validators.trimOrNull(request.getAlamat()));
        karyawan.setDob(Validators.requireNotFuture(request.getDob(), "dob", LocalDate.now()));
        karyawan.setStatus(Validators.requireText(request.getStatus(), "status"));
        karyawan.setUpdatedDate(now);

        return KaryawanResponse.from(karyawanRepository.save(karyawan));
    }

    /**
     * Baca pertama ambil dari database lalu disimpan ke Redis;
     * baca berikutnya (id yang sama) langsung dilayani dari Redis sampai TTL habis atau di-evict.
     */
    @Override
    @Cacheable(cacheNames = CACHE_KARYAWAN, key = "#id")
    public KaryawanResponse findById(Integer id) {
        return KaryawanResponse.from(requireActive(id));
    }

    @Override
    public Page<KaryawanResponse> findAll(Pageable pageable)  {
        return karyawanRepository.findAllByDeletedDateIsNull(pageable).map(KaryawanResponse::from);
    }

    @Override
    public Page<KaryawanResponse> findPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
        return karyawanRepository.findAllByDeletedDateIsNull(pageable).map(KaryawanResponse::from);
    }

    @Override
    public Page<KaryawanResponse> findPageByNama(String nama, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
        Page<Karyawan> pageKaryawan = karyawanRepository.findAllByNamaContainingIgnoreCaseAndDeletedDateIsNull(nama, pageable);

        List<KaryawanResponse> karyawanResponses = pageKaryawan.getContent().stream()
                .map(KaryawanResponse::from)
                .toList();

        return new PageImpl<>(
                karyawanResponses,
                pageable,
                pageKaryawan.getTotalElements()
        );
    }

    /**
     * Seluruh daftar disimpan sebagai satu entri cache dengan key tetap "all".
     * Karena isinya bisa berubah oleh perubahan karyawan mana pun, setiap operasi tulis
     * membuang entri ini (lihat anotasi {@code @Caching} di method create/update/delete).
     *
     * <p>Sengaja mengumpulkan ke {@link ArrayList}, bukan {@code .toList()}: {@code List.of}
     * dan {@code Stream.toList()} menghasilkan class immutable yang {@code final}, sehingga
     * Jackson tidak menulis informasi tipe dan datanya gagal dibaca kembali dari Redis.
     */
    @Override
    @Cacheable(cacheNames = CACHE_KARYAWAN_ALL, key = "'all'")
    public List<KaryawanResponse> findAll() {
        return karyawanRepository.findAllByDeletedDateIsNullOrderByIdAsc().stream()
                .map(KaryawanResponse::from)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_KARYAWAN, key = "#id"),
            @CacheEvict(cacheNames = CACHE_KARYAWAN_ALL, key = "'all'")
    })
    public void delete(Integer id) {
        Karyawan karyawan = requireActive(id);
        Instant now = Instant.now();

        karyawan.setDeletedDate(now);
        karyawan.setUpdatedDate(now);
        karyawanRepository.save(karyawan);

        DetailKaryawan detail = karyawan.getDetailKaryawan();
        if (detail != null && detail.getDeletedDate() == null) {
            detailKaryawanWriter.softDelete(detail, now);
        }

        rekeningRepository.softDeleteByKaryawanId(id, now);
        karyawanTrainingRepository.softDeleteByKaryawanId(id, now);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_KARYAWAN, key = "#karyawanId"),
            @CacheEvict(cacheNames = CACHE_KARYAWAN_ALL, key = "'all'")
    })
    public KaryawanResponse upsertDetail(Integer karyawanId, DetailKaryawanRequest request) {
        Karyawan karyawan = requireActive(karyawanId);
        Instant now = Instant.now();
        DetailKaryawan detail = karyawan.getDetailKaryawan();

        if (detail == null || detail.getDeletedDate() != null) {
            karyawan.setDetailKaryawan(detailKaryawanWriter.create(request, now));
        } else {
            detailKaryawanWriter.update(detail, request, now);
        }

        karyawan.setUpdatedDate(now);
        return KaryawanResponse.from(karyawanRepository.save(karyawan));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_KARYAWAN, key = "#karyawanId"),
            @CacheEvict(cacheNames = CACHE_KARYAWAN_ALL, key = "'all'")
    })
    public KaryawanResponse removeDetail(Integer karyawanId) {
        Karyawan karyawan = requireActive(karyawanId);
        Instant now = Instant.now();
        DetailKaryawan detail = karyawan.getDetailKaryawan();

        if (detail != null && detail.getDeletedDate() == null) {
            detailKaryawanWriter.softDelete(detail, now);
        }

        karyawan.setDetailKaryawan(null);
        karyawan.setUpdatedDate(now);
        return KaryawanResponse.from(karyawanRepository.save(karyawan));
    }

    private Karyawan requireActive(Integer id) {
        Validators.requireNotNull(id, "id karyawan");
        return karyawanRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }
}
