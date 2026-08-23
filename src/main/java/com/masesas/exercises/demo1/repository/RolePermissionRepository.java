package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.RolePermission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Integer> {

    @EntityGraph(attributePaths = {"role", "permission"})
    List<RolePermission> findAllByRole_IdIn(Collection<Integer> idRole);

    @EntityGraph(attributePaths = {"role", "permission"})
    List<RolePermission> findAllBy();

    @EntityGraph(attributePaths = {"role", "permission"})
    List<RolePermission> findAllByRole_Id(Integer idRole);

    Optional<RolePermission> findByRole_IdAndPermission_Id(Integer idRole, Integer idPermission);

    boolean existsByPermission_Id(Integer idPermission);
}
