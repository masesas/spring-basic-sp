package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Penetapan plafond kredit satu customer")
public class LoanPlafondRequest {

    @NotNull
    @Schema(description = "ID customer", example = "1")
    private Integer idCustomer;

    @NotNull
    @DecimalMin("0.0")
    @Schema(description = "Batas kredit yang diberikan", example = "50000000.00")
    private BigDecimal plafondTotal;
}
