package com.masesas.exercises.demo1.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayrollUpdateRequest {

    private Long version;

    @PositiveOrZero(message = "gajiPokok tidak boleh negatif")
    private BigDecimal gajiPokok;

    @PositiveOrZero(message = "tunjangan tidak boleh negatif")
    private BigDecimal tunjangan;

    @PositiveOrZero(message = "potongan tidak boleh negatif")
    private BigDecimal potongan;
}
