package com.masesas.exercises.demo1.config.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.List;

@Component
@Data
@ConfigurationProperties(prefix = "app")
public class AppConfigProperties {

    private Security security;
    private Redis redis;
    private DummyJson dummyJson;
    private Image image;

    @Data
    public static class Security {
        private String password;
        private String jwtSecret;
        private Long jwtTtlMinutes;
        private String cryptoKey;
        private List<String> corsAllowedOrigins;
    }

    @Data
    public static class Redis {
        private String keyPrefix;
        private String host;
        private Integer port;
        private Duration timeout;
        private String username;
        private String password;
        private Integer lettucePoolMaxActive;
        private Duration lettucePoolMaxWait;
        private Integer lettucePoolMaxIdle;
        private Integer lettucePoolMinIdle;
    }

    @Data
    public static class DummyJson {
        private String baseUrl;
        private Duration connectTimeout;
        private Duration readTimeout;
    }

    @Data
    public static class Image {
        private String baseDir;
        private DataSize maxSize;
    }
}
