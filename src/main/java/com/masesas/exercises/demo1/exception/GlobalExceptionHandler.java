package com.masesas.exercises.demo1.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.Map;

/**
 * Memetakan exception domain ke status HTTP yang tepat. Tanpa ini semuanya
 * jatuh ke 500, termasuk kasus wajar seperti data tidak ditemukan.
 *
 * <p>Kelas ini mewarisi {@link ResponseEntityExceptionHandler} dengan sengaja. Tanpa itu,
 * handler {@code Exception.class} di bawah ikut menelan exception bawaan Spring MVC
 * (misal {@code NoResourceFoundException} untuk URL tak dikenal) dan mengubah 404 jadi 500.
 * Superclass-nya sudah menangani exception standar tersebut secara spesifik, dan Spring
 * selalu memilih handler yang paling spesifik.
 *
 * <p>Pesan detail hanya dikirim untuk exception domain yang memang kita rancang aman dibaca
 * klien. Exception tak terduga dicatat lengkap di log, tapi klien hanya menerima pesan generik.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalid(InvalidRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    /**
     * Tanpa handler ini, {@code AccessDeniedException} dari {@code @PreAuthorize} tertelan
     * handler {@code Exception.class} di bawah dan berubah jadi 500 — penolakan otorisasi
     * jadi tampak seperti kerusakan server.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Akses ditolak");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Autentikasi diperlukan");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Kesalahan tak terduga", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Terjadi kesalahan pada server");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? "" : message));
    }
}
