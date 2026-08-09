package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.CustomerRegisterRequest;
import com.masesas.exercises.demo1.dto.CustomerResponse;
import com.masesas.exercises.demo1.entity.Customer;
import com.masesas.exercises.demo1.exception.DuplicateResourceException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.CustomerRepository;
import com.masesas.exercises.demo1.repository.KaryawanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final KaryawanRepository karyawanRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional
    public CustomerResponse register(CustomerRegisterRequest request) {
        String email = request.getEmail();
        if (customerRepository.existsByEmail(email) || karyawanRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email sudah terdaftar");
        }

        Instant sekarang = Instant.now(clock);
        Customer customer = new Customer();
        customer.setNama(request.getNama());
        customer.setEmail(email);
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setCreatedDate(sekarang);
        customer.setUpdatedDate(sekarang);

        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public CustomerResponse profil(String email) {
        return customerRepository.findByEmailAndDeletedDateIsNull(email)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", email));
    }
}
