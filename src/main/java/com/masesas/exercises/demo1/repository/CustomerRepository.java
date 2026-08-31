package com.masesas.exercises.demo1.repository;

import com.masesas.exercises.demo1.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByEmailAndDeletedDateIsNull(String email);

    Page<Customer> findAllByDeletedDateIsNull(Pageable pageable);

    boolean existsByEmail(String email);
}
