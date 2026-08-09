package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.KaryawanRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KaryawanRoleRepository extends JpaRepository<KaryawanRole, Integer> {

    @EntityGraph(attributePaths = "role")
    List<KaryawanRole> findAllByKaryawan_Id(Integer idKaryawan);
}
