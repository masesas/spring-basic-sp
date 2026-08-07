package com.masesas.exercises.demo1.service.support;

import com.masesas.exercises.demo1.exception.InvalidRequestException;

import java.time.LocalDate;

public final class Validators {

    private Validators() {
    }

    public static <T> T requireNotNull(T value, String field) {
        if (value == null) {
            throw new InvalidRequestException(field + " wajib diisi");
        }
        return value;
    }

    public static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(field + " wajib diisi");
        }
        return value.trim();
    }

    public static LocalDate requireNotFuture(LocalDate value, String field, LocalDate today) {
        requireNotNull(value, field);
        if (value.isAfter(today)) {
            throw new InvalidRequestException(field + " tidak boleh melebihi tanggal hari ini");
        }
        return value;
    }

    public static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
