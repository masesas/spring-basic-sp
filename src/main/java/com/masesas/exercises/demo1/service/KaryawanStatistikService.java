package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.model.StatistikKaryawan;
import com.masesas.exercises.demo1.repository.storeprocedure.KaryawanStatistikRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service untuk stored procedure ber-OUT parameter. */
@Service
@RequiredArgsConstructor
public class KaryawanStatistikService {

    private final KaryawanStatistikRepository repository;

    /** Wajib @Transactional: nilai OUT parameter hanya bisa dibaca selama transaksi masih terbuka. */
    @Transactional
    public Integer totalByStatus(String status) {
        return repository.getTotalByStatus(status);
    }

    /** Statistik umur karyawan; Map dari OUT parameter diubah menjadi DTO. */
    @Transactional
    public StatistikKaryawan statistikByStatus(String status) {
        return StatistikKaryawan.fromMap(repository.getStatistikByStatus(status));
    }
}
