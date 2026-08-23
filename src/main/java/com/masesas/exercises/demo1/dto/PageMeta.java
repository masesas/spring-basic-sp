package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PageMeta", description = "Informasi paginasi, hanya muncul pada endpoint berpaginasi")
public class PageMeta {

    @Schema(description = "Nomor halaman saat ini, dimulai dari 0", example = "0")
    private int page;

    @Schema(description = "Jumlah baris per halaman", example = "10")
    private int size;

    @Schema(description = "Jumlah seluruh baris pada semua halaman", example = "57")
    private long totalElements;

    @Schema(description = "Jumlah halaman yang tersedia", example = "6")
    private int totalPages;
}
