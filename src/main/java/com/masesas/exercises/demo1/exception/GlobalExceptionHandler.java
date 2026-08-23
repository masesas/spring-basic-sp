package com.masesas.exercises.demo1.exception;

import com.masesas.exercises.demo1.dto.BaseApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String PESAN_AUTENTIKASI_DIPERLUKAN = "Autentikasi diperlukan";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String JSON = "application/json";
    private static final String KODE_VALIDASI = "VALIDATION_ERROR";

    @ExceptionHandler(ResourceNotFoundException.class)
    @ApiResponse(
            responseCode = "404",
            description = "Data yang diminta tidak ada",
            content = @Content(mediaType = JSON,
                    schema = @Schema(implementation = BaseApiResponse.class)))
    public ResponseEntity<BaseApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), "NOT_FOUND");
    }

    @ExceptionHandler({ConflictException.class, ObjectOptimisticLockingFailureException.class})
    @ApiResponse(
            responseCode = "409",
            description = "Bentrok dengan data yang sudah ada, atau data sudah diubah orang lain",
            content = @Content(mediaType = JSON,
                    schema = @Schema(implementation = BaseApiResponse.class)))
    public ResponseEntity<BaseApiResponse<Object>> handleConflict(Exception ex) {
        String pesan = ex instanceof ConflictException
                ? ex.getMessage()
                : "Data sudah diubah orang lain. Muat ulang lalu coba lagi.";
        return build(HttpStatus.CONFLICT, pesan, "CONFLICT");
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<BaseApiResponse<Object>> handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "DUPLICATE_RESOURCE");
    }

    @ExceptionHandler(InvalidRequestException.class)
    @ApiResponse(
            responseCode = "400",
            description = "Isi permintaan tidak lolos validasi",
            content = @Content(mediaType = JSON,
                    schema = @Schema(implementation = BaseApiResponse.class)))
    public ResponseEntity<BaseApiResponse<Object>> handleInvalid(InvalidRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_REQUEST");
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ApiResponse(
            responseCode = "422",
            description = "Bentuk permintaan benar, tapi melanggar aturan bisnis",
            content = @Content(mediaType = JSON,
                    schema = @Schema(implementation = BaseApiResponse.class)))
    public ResponseEntity<BaseApiResponse<Object>> handleBusinessRule(BusinessRuleException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), "BUSINESS_RULE_VIOLATION");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<String> rincian = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return new ResponseEntity<>(
                BaseApiResponse.error(
                        HttpStatus.BAD_REQUEST, String.join("; ", rincian), KODE_VALIDASI, rincian),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> rincian = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());
        return ResponseEntity.badRequest().body(
                BaseApiResponse.error(
                        HttpStatus.BAD_REQUEST, String.join("; ", rincian), KODE_VALIDASI, rincian));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ApiResponse(
            responseCode = "403",
            description = "Sudah login, tapi peran akun tidak cukup untuk endpoint ini",
            content = @Content(mediaType = JSON,
                    schema = @Schema(implementation = BaseApiResponse.class)))
    public ResponseEntity<BaseApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Akses ditolak", "ACCESS_DENIED");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ApiResponse(
            responseCode = "401",
            description = "Token tidak ada, tidak valid, atau kedaluwarsa",
            content = @Content(mediaType = JSON,
                    schema = @Schema(implementation = BaseApiResponse.class)))
    public ResponseEntity<BaseApiResponse<Object>> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, PESAN_AUTENTIKASI_DIPERLUKAN, "UNAUTHORIZED");
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<BaseApiResponse<Object>> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), "UNAUTHORIZED");
    }

    @ExceptionHandler(Exception.class)
    @ApiResponse(
            responseCode = "500",
            description = "Kesalahan tak terduga di server",
            content = @Content(mediaType = JSON,
                    schema = @Schema(implementation = BaseApiResponse.class)))
    public ResponseEntity<BaseApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("Kesalahan tak terduga", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Terjadi kesalahan pada server", "INTERNAL_ERROR");
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        if (body instanceof BaseApiResponse) {
            return super.handleExceptionInternal(ex, body, headers, statusCode, request);
        }
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        return super.handleExceptionInternal(
                ex,
                BaseApiResponse.error(status, pesanBawaan(ex, body, status), status.name()),
                headers,
                statusCode,
                request);
    }

    private String pesanBawaan(Exception ex, Object body, HttpStatus status) {
        if (body instanceof ProblemDetail detail && detail.getDetail() != null) {
            return detail.getDetail();
        }
        if (ex instanceof ErrorResponse response && response.getBody().getDetail() != null) {
            return response.getBody().getDetail();
        }
        return status.getReasonPhrase();
    }

    private ResponseEntity<BaseApiResponse<Object>> build(HttpStatus status, String message, String code) {
        return ResponseEntity.status(status).body(BaseApiResponse.error(status, message, code));
    }
}
