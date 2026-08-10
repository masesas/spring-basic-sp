package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.model.HasilProsesKaryawan;
import com.masesas.exercises.demo1.model.ProsesKaryawanRequest;
import com.masesas.exercises.demo1.repository.storeprocedure.KaryawanPostgresJpaProcedureRepository;
import com.masesas.exercises.demo1.repository.storeprocedure.KaryawanPostgresProcedureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Service untuk sp_proses_karyawan; membandingkan pemanggilan lewat JdbcTemplate dan JPA. */
@Service
@RequiredArgsConstructor
public class KaryawanSpService {

    private static final String MODE_DEFAULT = "RINGKAS";

    private final KaryawanPostgresProcedureRepository jdbcRepository;
    private final KaryawanPostgresJpaProcedureRepository jpaRepository;

    /** Hanya membaca data: semua parameter update dikirim null. */
    public HasilProsesKaryawan lihatViaJdbc(Integer id, String mode) {
        return jdbcRepository.proses(id, null, null, null, modeAtauDefault(mode));
    }

    /** Hanya membaca data: semua parameter update dikirim null. */
    public HasilProsesKaryawan lihatViaJpa(Integer id, String mode) {
        return jpaRepository.proses(id, null, null, null, modeAtauDefault(mode));
    }

    /** Update sekaligus baca: SP mengubah data lalu mengembalikan tiga result set. */
    public HasilProsesKaryawan prosesViaJdbc(Integer id, ProsesKaryawanRequest request) {
        return jdbcRepository.proses(id, request.getNama(), request.getAlamat(), request.getStatus(),
                modeAtauDefault(request.getMode()));
    }

    /** Update sekaligus baca: SP mengubah data lalu mengembalikan tiga result set. */
    public HasilProsesKaryawan prosesViaJpa(Integer id, ProsesKaryawanRequest request) {
        return jpaRepository.proses(id, request.getNama(), request.getAlamat(), request.getStatus(),
                modeAtauDefault(request.getMode()));
    }

    private String modeAtauDefault(String mode) {
        return mode == null || mode.isBlank() ? MODE_DEFAULT : mode;
    }
}
