package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    Page<Permission> findAllByDeletedDateIsNull(Pageable pageable);

    Optional<Permission> findByIdAndDeletedDateIsNull(Integer id);

    boolean existsByKodeIgnoreCaseAndDeletedDateIsNull(String kode);

    boolean existsByKodeIgnoreCaseAndDeletedDateIsNullAndIdNot(String kode, Integer id);
}
