package com.masesas.exercises.demo1.owasp.safe;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String[] PATH_DIBATASI = {
            "/api/safe/login",
            "/api/safe/karyawan/search"
    };

    private final Map<String, Bucket> perKlien = new ConcurrentHashMap<>();
    private final int kapasitas;

    public RateLimitFilter(@Value("${app.security.rate-limit-per-minute}") int kapasitas) {
        this.kapasitas = kapasitas;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String dibatasi : PATH_DIBATASI) {
            if (path.equals(dibatasi)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Bucket bucket = perKlien.computeIfAbsent(kunci(request), key -> buatBucket());
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "60");
    }

    public void bersihkan() {
        perKlien.clear();
    }

    private Bucket buatBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(kapasitas)
                        .refillIntervally(kapasitas, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private String kunci(HttpServletRequest request) {
        return request.getRequestURI() + "|" + request.getRemoteAddr();
    }
}
