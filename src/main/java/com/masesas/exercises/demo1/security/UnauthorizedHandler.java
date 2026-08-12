package com.masesas.exercises.demo1.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UnauthorizedHandler implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    private static final String PESAN_AUTENTIKASI_DIPERLUKAN = "Autentikasi diperlukan";

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException ex) throws IOException {
        response(response, PESAN_AUTENTIKASI_DIPERLUKAN);
    }

    public void response(HttpServletResponse response, String pesan) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getWriter(),
                body(HttpStatus.UNAUTHORIZED, pesan));
    }

    private Map<String, Object> body(HttpStatus status, String message) {
        return Map.of(
                "timestamp", System.currentTimeMillis(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? "" : message);
    }
}
