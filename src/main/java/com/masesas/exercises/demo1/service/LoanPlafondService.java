package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.LoanPlafondRequest;
import com.masesas.exercises.demo1.dto.LoanPlafondResponse;
import com.masesas.exercises.demo1.entity.Customer;
import com.masesas.exercises.demo1.entity.LoanPlafond;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.CustomerRepository;
import com.masesas.exercises.demo1.repository.LoanPlafondRepository;
import com.masesas.exercises.demo1.service.support.Validators;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanPlafondService {

    private final LoanPlafondRepository loanPlafondRepository;
    private final CustomerRepository customerRepository;
    private final Clock clock;

    @Transactional
    public LoanPlafondResponse tetapkan(LoanPlafondRequest request) {
        Validators.requireNotNull(request, "data plafond");
        Customer customer = requireCustomer(request.getIdCustomer());
        Instant sekarang = Instant.now(clock);

        LoanPlafond plafond = loanPlafondRepository.findByCustomer_Id(customer.getId())
                .orElseGet(() -> LoanPlafond.baru(customer, request.getPlafondTotal(), sekarang));

        if (plafond.getId() != null) {
            plafond.ubahTotal(request.getPlafondTotal(), sekarang);
        }

        return LoanPlafondResponse.from(loanPlafondRepository.saveAndFlush(plafond));
    }

    public LoanPlafondResponse findByCustomer(Integer idCustomer) {
        return LoanPlafondResponse.from(requirePlafond(idCustomer));
    }

    public Page<LoanPlafondResponse> findAll(Pageable pageable) {
        return loanPlafondRepository.findAllBy(pageable).map(LoanPlafondResponse::from);
    }

    LoanPlafond requirePlafond(Integer idCustomer) {
        Validators.requireNotNull(idCustomer, "id customer");
        return loanPlafondRepository.findByCustomer_Id(idCustomer)
                .orElseThrow(() -> new ResourceNotFoundException("LoanPlafond", "customer=" + idCustomer));
    }

    private Customer requireCustomer(Integer idCustomer) {
        Validators.requireNotNull(idCustomer, "id customer");
        return customerRepository.findById(idCustomer)
                .filter(customer -> customer.getDeletedDate() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", idCustomer));
    }
}
