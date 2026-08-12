package com.masesas.exercises.demo1.dummyjsondto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductReview {

    private Integer rating;
    private String comment;
    private String date;
    private String reviewerName;
    private String reviewerEmail;
}
