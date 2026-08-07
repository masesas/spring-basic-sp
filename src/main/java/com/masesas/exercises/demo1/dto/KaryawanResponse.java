package com.masesas.exercises.demo1.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class KaryawanResponse {
    private Integer id;
    private String nama;
    private String alamat;
    private LocalDate dob;
    private String status;
    private DetailKaryawanResponse detail;
    private Instant createdDate;
    private Instant updatedDate;
}
