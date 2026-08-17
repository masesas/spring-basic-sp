package com.masesas.exercises.demo1.exception;

import com.masesas.exercises.demo1.dto.ApiErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String PESAN_AUTENTIKASI_DIPERLUKAN = "Autentikasi diperlukan";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String JSON = "application/json";

    @ExceptionHandler(ResourceNotFoundException.class)
    @ApiResponse(
            responseCode = "404",
            description = "Data yang diminta tidak ada",
            content = @Content(mediaType = JSON,
                    schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({ConflictException.class, ObjectOptimisticLockingFailureException.class})
    @ApiResponse(
            responseCode = "409",
            description = "Bentrok dengan data yang sudah ada, atau data sudah diubah orang lain",
            content = @Content(mediaType = JSON,
                    schema = @Schema(implementation = ApiErrorResponse.class)))
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
    @ApiResponse(
            responseCode = "400",
            description = "Isi permintaan tidak lolos validasi",
            content = @Content(mediaType = JSON,
                    schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Map<String, Object>> handleInvalid(InvalidRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ApiResponse(
            responseCode = "422",
            description = "Bentuk permintaan benar, tapi melanggar aturan bisnis",
            content = @Content(mediaType = JSON,
                    schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

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

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String pesan = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, pesan);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ApiResponse(
            responseCode = "403",
            description = "Sudah login, tapi peran akun tidak cukup untuk endpoint ini",
            content = @Content(mediaType = JSON,
                    schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Akses ditolak");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ApiResponse(
            responseCode = "401",
            description = "Token tidak ada, tidak valid, atau kedaluwarsa",
            content = @Content(mediaType = JSON,
                    schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, PESAN_AUTENTIKASI_DIPERLUKAN);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ApiResponse(
            responseCode = "500",
            description = "Kesalahan tak terduga di server",
            content = @Content(mediaType = JSON,
                    schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Kesalahan tak terduga", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Terjadi kesalahan pada server");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(body(status, message));
    }

    public static Map<String, Object> body(HttpStatus status, String message) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? "" : message);
    }
}
