package com.masesas.exercises.demo1.dummyjsondto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductMeta {

    private String createdAt;
    private String updatedAt;
    private String barcode;
    private String qrCode;
}
