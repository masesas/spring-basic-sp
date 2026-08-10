package com.masesas.exercises.demo1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Karyawan2Request {

    private String nama;
    private String alamat;
    private LocalDate dob;
    private String status;
}
