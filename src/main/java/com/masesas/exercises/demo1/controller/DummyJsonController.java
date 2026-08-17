package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dummyjsondto.ProductResponse;
import com.masesas.exercises.demo1.dummyjsondto.ProductSearchResponse;
import com.masesas.exercises.demo1.service.DummyJsonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dummyjson")
@RequiredArgsConstructor
@Tag(name = "DummyJson", description = "Contoh pemanggilan API pihak ketiga (dummyjson.com) "
        + "lewat backend. Butuh token, peran apa pun.")
@SecurityRequirement(name = "karyawanAuth")
@SecurityRequirement(name = "customerAuth")
public class DummyJsonController {

    private final DummyJsonService dummyJsonService;

    @GetMapping("/products/search")
    @Operation(
            summary = "Cari produk di dummyjson.com",
            description = "Tanpa parameter q, seluruh produk dikembalikan.")
    @ApiResponse(responseCode = "200", description = "Hasil pencarian")
    public ProductSearchResponse cariProduk(
            @Parameter(description = "Kata kunci pencarian", example = "phone")
            @RequestParam(required = false) String q) {
        return dummyJsonService.cariProduk(q);
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Detail satu produk dummyjson.com")
    @ApiResponse(responseCode = "200", description = "Produk ditemukan")
    public ProductResponse detailProduk(
            @Parameter(description = "ID produk di dummyjson.com", example = "1")
            @PathVariable Integer id) {
        return dummyJsonService.ambilProduk(id);
    }
}
