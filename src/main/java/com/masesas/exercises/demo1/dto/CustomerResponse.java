package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomerResponse {

    private final Integer id;
    private final String nama;
    private final String email;

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getNama(), customer.getEmail());
    }
}
