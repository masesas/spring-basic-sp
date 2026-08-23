package com.masesas.exercises.demo1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BaseApiResponse", description = "Bentuk baku badan response untuk seluruh endpoint")
public class BaseApiResponse<T> {

    @Schema(description = "Kode status HTTP, sama dengan status pada baris response", example = "200")
    private int statusCode;

    @Schema(description = "Penjelasan singkat hasil pemrosesan", example = "Karyawan ditemukan")
    private String message;

    @Schema(description = "Muatan hasil, bernilai null ketika permintaan gagal")
    private T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PageMeta meta;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ApiError error;

    public static <T> BaseApiResponse<T> ok(String message, T data) {
        return new BaseApiResponse<>(HttpStatus.OK.value(), message, data, null, null);
    }

    public static <T> BaseApiResponse<T> created(String message, T data) {
        return new BaseApiResponse<>(HttpStatus.CREATED.value(), message, data, null, null);
    }

    public static <T> BaseApiResponse<List<T>> page(String message, Page<T> halaman) {
        PageMeta meta = new PageMeta(
                halaman.getNumber(),
                halaman.getSize(),
                halaman.getTotalElements(),
                halaman.getTotalPages());
        return new BaseApiResponse<>(HttpStatus.OK.value(), message, halaman.getContent(), meta, null);
    }

    public static <T> BaseApiResponse<T> error(HttpStatus status, String message, String code) {
        return error(status, message, code, null);
    }

    public static <T> BaseApiResponse<T> error(HttpStatus status, String message, String code, List<String> details) {
        return new BaseApiResponse<>(
                status.value(),
                message == null ? status.getReasonPhrase() : message,
                null,
                null,
                new ApiError(code, details));
    }
}
