package com.masesas.exercises.demo1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProsesKaryawanRequest {

    private String nama;
    private String alamat;
    private String status;
    private String mode;
}
