package com.masesas.exercises.demo1.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * Dua sumber konflik yang berbeda, keduanya bermuara ke 409.
     *
     * <p>{@link ConflictException} muncul saat klien mengirim {@code version} yang sudah
     * basi — dia mengedit berdasarkan data yang keburu diubah orang lain.
     * {@link ObjectOptimisticLockingFailureException} muncul saat dua transaksi menulis
     * baris yang sama benar-benar bersamaan dan Hibernate mendeteksinya lewat kolom
     * {@code version}.
     */
    @ExceptionHandler({ConflictException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(Exception ex) {
        String pesan = ex instanceof ConflictException
                ? ex.getMessage()
                : "Data sudah diubah orang lain. Muat ulang lalu coba lagi.";
        return build(HttpStatus.CONFLICT, pesan);
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
     * Bean Validation pada {@code @RequestBody}. Superclass memetakannya ke 400 tapi dengan
     * body ProblemDetail bawaan Spring; di sini dipaksa memakai amplop yang sama dengan
     * handler lain, dan pesannya menyebut field mana yang bermasalah.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String pesan = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new ResponseEntity<>(body(HttpStatus.BAD_REQUEST, pesan), HttpStatus.BAD_REQUEST);
    }

    /** Bean Validation pada {@code @RequestParam}/{@code @PathVariable} kelas ber-{@code @Validated}. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String pesan = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, pesan);
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
        return ResponseEntity.status(status).body(body(status, message));
    }

    private Map<String, Object> body(HttpStatus status, String message) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? "" : message);
    }
}
