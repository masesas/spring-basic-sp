package com.masesas.exercises.demo1.config.prop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Component
@Data
@Validated
@ConfigurationProperties(prefix = "app")
public class AppConfigProperties {

    private static final String BUKAN_PLACEHOLDER = "^(?!\\$\\{).+";
    private static final String PESAN_PLACEHOLDER =
            "masih berbentuk placeholder: environment variable yang dirujuk belum diisi";

    @Valid
    @NotNull
    private Security security;

    @Valid
    @NotNull
    private Redis redis;

    @Valid
    @NotNull
    private DummyJson dummyJson;

    @Valid
    @NotNull
    private Image image;

    @Data
    public static class Security {
        @NotBlank
        @Pattern(regexp = BUKAN_PLACEHOLDER, message = PESAN_PLACEHOLDER)
        private String password;

        @NotBlank
        @Pattern(regexp = BUKAN_PLACEHOLDER, message = PESAN_PLACEHOLDER)
        private String jwtSecret;

        @NotNull
        private Long jwtTtlMinutes;

        @NotBlank
        @Pattern(regexp = BUKAN_PLACEHOLDER, message = PESAN_PLACEHOLDER)
        private String cryptoKey;

        @NotEmpty
        private List<String> corsAllowedOrigins;
    }

    @Data
    public static class Redis {
        @NotBlank
        @Pattern(regexp = BUKAN_PLACEHOLDER, message = PESAN_PLACEHOLDER)
        private String keyPrefix;

        @NotBlank
        @Pattern(regexp = BUKAN_PLACEHOLDER, message = PESAN_PLACEHOLDER)
        private String host;

        @NotNull
        private Integer port;

        @NotNull
        private Duration timeout;

        @NotBlank
        @Pattern(regexp = BUKAN_PLACEHOLDER, message = PESAN_PLACEHOLDER)
        private String username;

        @NotBlank
        @Pattern(regexp = BUKAN_PLACEHOLDER, message = PESAN_PLACEHOLDER)
        private String password;

        private Integer lettucePoolMaxActive;
        private Duration lettucePoolMaxWait;
        private Integer lettucePoolMaxIdle;
        private Integer lettucePoolMinIdle;
    }

    @Data
    public static class DummyJson {
        @NotBlank
        @Pattern(regexp = BUKAN_PLACEHOLDER, message = PESAN_PLACEHOLDER)
        private String baseUrl;

        private Duration connectTimeout;
        private Duration readTimeout;
    }

    @Data
    public static class Image {
        @NotBlank
        @Pattern(regexp = BUKAN_PLACEHOLDER, message = PESAN_PLACEHOLDER)
        private String baseDir;

        private DataSize maxSize;
    }
}
