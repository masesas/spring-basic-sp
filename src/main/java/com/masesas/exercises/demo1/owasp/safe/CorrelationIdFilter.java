package com.masesas.exercises.demo1.owasp.safe;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final int PANJANG_MAKS = 64;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String correlationId = dariKlienAtauBaru(request.getHeader(HEADER));
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * Nilai dari klien tidak pernah dipercaya mentah: ia masuk ke setiap baris log,
     * jadi baris baru di dalamnya bisa dipakai memalsukan entri log. Panjangnya juga
     * dibatasi supaya satu request tidak bisa membanjiri berkas log.
     */
    private String dariKlienAtauBaru(String dariKlien) {
        if (dariKlien == null || dariKlien.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String bersih = InputSanitizer.untukLog(dariKlien.trim());
        return bersih.length() > PANJANG_MAKS ? bersih.substring(0, PANJANG_MAKS) : bersih;
    }
}
