package com.masesas.exercises.demo1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RekeningRequest {

    private Integer idKaryawan;
    private String jenis;
    private String nama;
    private String rekening;
}
