package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {
}
