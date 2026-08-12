package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.config.prop.AppConfigProperties;
import com.masesas.exercises.demo1.dummyjsondto.ProductResponse;
import com.masesas.exercises.demo1.dummyjsondto.ProductSearchResponse;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class DummyJsonService {

    private final RestTemplate dummyJsonRestTemplate;
    private final AppConfigProperties properties;

    public ProductSearchResponse cariProduk(String q) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(properties.getDummyJson().getBaseUrl())
                .path("/products/search");
        if (q != null && !q.isBlank()) {
            builder.queryParam("q", q);
        }
        URI uri = builder.encode().build().toUri();
        return dummyJsonRestTemplate.getForObject(uri, ProductSearchResponse.class);
    }

    public ProductResponse ambilProduk(Integer id) {
        URI uri = UriComponentsBuilder
                .fromUriString(properties.getDummyJson().getBaseUrl())
                .path("/products/{id}")
                .buildAndExpand(id)
                .encode()
                .toUri();
        try {
            return dummyJsonRestTemplate.getForObject(uri, ProductResponse.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Produk", id);
        }
    }
}
