package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dummyjsondto.ProductResponse;
import com.masesas.exercises.demo1.dummyjsondto.ProductSearchResponse;
import com.masesas.exercises.demo1.service.DummyJsonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dummyjson")
@RequiredArgsConstructor
public class DummyJsonController {

    private final DummyJsonService dummyJsonService;

    @GetMapping("/products/search")
    public ProductSearchResponse cariProduk(@RequestParam(required = false) String q) {
        return dummyJsonService.cariProduk(q);
    }

    @GetMapping("/products/{id}")
    public ProductResponse detailProduk(@PathVariable Integer id) {
        return dummyJsonService.ambilProduk(id);
    }
}
