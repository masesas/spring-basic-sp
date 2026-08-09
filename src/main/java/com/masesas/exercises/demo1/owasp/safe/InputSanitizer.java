package com.masesas.exercises.demo1.owasp.safe;

import com.masesas.exercises.demo1.exception.InvalidRequestException;

import java.util.regex.Pattern;

public final class InputSanitizer {

    private static final Pattern MENGANDUNG_MARKUP = Pattern.compile("[<>]");
    private static final Pattern SKEMA_SCRIPT = Pattern.compile("(?i)javascript:|data:text/html");
    private static final Pattern PEMISAH_BARIS = Pattern.compile("[\\r\\n]");

    private InputSanitizer() {
    }

    public static String requireBebasMarkup(String value, String field) {
        if (value == null) {
            return null;
        }
        if (MENGANDUNG_MARKUP.matcher(value).find() || SKEMA_SCRIPT.matcher(value).find()) {
            throw new InvalidRequestException(field + " tidak boleh memuat markup HTML atau skrip");
        }
        return value;
    }

    public static String untukLog(String value) {
        if (value == null) {
            return "";
        }
        return PEMISAH_BARIS.matcher(value).replaceAll("_");
    }
}
