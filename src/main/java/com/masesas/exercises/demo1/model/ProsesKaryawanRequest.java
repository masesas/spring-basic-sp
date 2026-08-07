package com.masesas.exercises.demo1.model;

/** Body request untuk sp_proses_karyawan; field null berarti kolomnya tidak diubah. */
public record ProsesKaryawanRequest(String nama, String alamat, String status, String mode) {
}
