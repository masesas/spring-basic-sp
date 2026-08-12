package com.masesas.exercises.demo1.dummyjsondto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductSearchResponse {

    private List<ProductResponse> products;
    private Integer total;
    private Integer skip;
    private Integer limit;
}
