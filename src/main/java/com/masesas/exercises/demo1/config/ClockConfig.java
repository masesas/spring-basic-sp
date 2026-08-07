package com.masesas.exercises.demo1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Sumber waktu tunggal untuk seluruh aplikasi.
 * Dibuat sebagai bean supaya di unit test bisa diganti dengan {@code Clock.fixed(...)}.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
