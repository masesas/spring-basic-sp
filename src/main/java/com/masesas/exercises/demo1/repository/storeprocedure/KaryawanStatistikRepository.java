package com.masesas.exercises.demo1.repository.storeprocedure;

import com.masesas.exercises.demo1.entity.Karyawan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import java.util.Map;

public interface KaryawanStatistikRepository extends JpaRepository<Karyawan, Integer> {
    @Procedure(name = "Karyawan.getTotalByStatus")
    Integer getTotalByStatus(@Param("status_in") String status);

    @Procedure(name = "Karyawan.getStatistikByStatus")
    Map<String, Object> getStatistikByStatus(@Param("status_in") String status);
}
