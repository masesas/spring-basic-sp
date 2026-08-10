package com.masesas.exercises.demo1.service.support;

import com.masesas.exercises.demo1.dto.DetailKaryawanRequest;
import com.masesas.exercises.demo1.entity.DetailKaryawan;
import com.masesas.exercises.demo1.exception.DuplicateResourceException;
import com.masesas.exercises.demo1.repository.DetailKaryawanRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DetailKaryawanWriter {

    private final DetailKaryawanRepository detailKaryawanRepository;

    public DetailKaryawanWriter(DetailKaryawanRepository detailKaryawanRepository) {
        this.detailKaryawanRepository = detailKaryawanRepository;
    }

    public DetailKaryawan create(DetailKaryawanRequest request, Instant timestamp) {
        Validators.requireNotNull(request, "detail karyawan");
        String nik = Validators.requireText(request.getNik(), "nik");
        String npwp = Validators.requireText(request.getNpwp(), "npwp");

        if (detailKaryawanRepository.existsByNikAndDeletedDateIsNull(nik)) {
            throw new DuplicateResourceException("NIK sudah terdaftar pada karyawan lain");
        }
        if (detailKaryawanRepository.existsByNpwpAndDeletedDateIsNull(npwp)) {
            throw new DuplicateResourceException("NPWP sudah terdaftar pada karyawan lain");
        }

        DetailKaryawan detail = new DetailKaryawan();
        detail.setNik(nik);
        detail.setNpwp(npwp);
        detail.setCreatedDate(timestamp);
        detail.setUpdatedDate(timestamp);
        return detailKaryawanRepository.save(detail);
    }

    public DetailKaryawan update(DetailKaryawan detail, DetailKaryawanRequest request, Instant timestamp) {
        Validators.requireNotNull(request, "detail karyawan");
        String nik = Validators.requireText(request.getNik(), "nik");
        String npwp = Validators.requireText(request.getNpwp(), "npwp");

        if (detailKaryawanRepository.existsByNikAndDeletedDateIsNullAndIdNot(nik, detail.getId())) {
            throw new DuplicateResourceException("NIK sudah terdaftar pada karyawan lain");
        }
        if (detailKaryawanRepository.existsByNpwpAndDeletedDateIsNullAndIdNot(npwp, detail.getId())) {
            throw new DuplicateResourceException("NPWP sudah terdaftar pada karyawan lain");
        }

        detail.setNik(nik);
        detail.setNpwp(npwp);
        detail.setUpdatedDate(timestamp);
        return detailKaryawanRepository.save(detail);
    }

    public void softDelete(DetailKaryawan detail, Instant timestamp) {
        detail.setDeletedDate(timestamp);
        detail.setUpdatedDate(timestamp);
        detailKaryawanRepository.save(detail);
    }
}
