package com.masesas.exercises.demo1.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Karyawan2Response {
    private Integer id;
    private String nama;
    private String alamat;
    private LocalDate dob;
    private  String status;

}
