package com.masesas.exercises.demo1.owasp.safe;

import com.masesas.exercises.demo1.exception.InvalidRequestException;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum SortField {

    ID("id"),
    NAMA("nama"),
    STATUS("status");

    private final String kolom;

    SortField(String kolom) {
        this.kolom = kolom;
    }

    public String kolom() {
        return kolom;
    }

    public static SortField dari(String input) {
        return Arrays.stream(values())
                .filter(field -> field.name().equalsIgnoreCase(input))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "kolom sort tidak dikenal: " + input + ". Pilihan: " + pilihan()));
    }

    private static String pilihan() {
        return Arrays.stream(values())
                .map(field -> field.name().toLowerCase())
                .collect(Collectors.joining(", "));
    }
}
