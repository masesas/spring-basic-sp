package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.masesas.exercises.demo1.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Identitas customer")
public class CustomerResponse {

    @Schema(description = "ID customer", example = "1")
    private final Integer id;
    @Schema(description = "Nama lengkap customer", example = "Siti Rahma")
    private final String nama;
    @Schema(description = "Email customer", example = "siti@contoh.test")
    private final String email;

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getNama(), customer.getEmail());
    }
}
