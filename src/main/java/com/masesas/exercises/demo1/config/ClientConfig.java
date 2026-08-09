package com.masesas.exercises.demo1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.io.IOException;
import java.time.Duration;

@Configuration
public class ClientConfig {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    /**
     * Klien tanpa batas waktu dan mengikuti redirect. Dipakai controller demo A10 yang
     * memang sengaja dibuat rentan.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Klien untuk memanggil alamat luar.
     *
     * <p>Redirect sengaja tidak diikuti. Tanpa itu, pemeriksaan {@link
     * com.masesas.exercises.demo1.owasp.safe.UrlGuard} bisa dilewati sepenuhnya: penyerang
     * cukup memberi alamat publik yang membalas 302 ke 127.0.0.1, dan yang diperiksa
     * hanyalah alamat pertama.
     *
     * <p>Batas waktu mencegah alamat yang menggantung menahan thread server selamanya —
     * cara murah membuat aplikasi berhenti melayani.
     */
    @Bean
    public RestTemplate restTemplateAman() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod)
                    throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(TIMEOUT);
        factory.setReadTimeout(TIMEOUT);
        return new RestTemplate(factory);
    }
}
