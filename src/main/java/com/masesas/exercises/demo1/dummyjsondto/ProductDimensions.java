package com.masesas.exercises.demo1.dummyjsondto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDimensions {

    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal depth;
}
