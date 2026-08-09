package com.masesas.exercises.demo1.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final Map<String, AppUser> users;
    private final String rawPassword;

    public AppUserDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${app.demo.password}") String rawPassword) {
        this.rawPassword = rawPassword;
        String encoded = passwordEncoder.encode(rawPassword);

        Map<String, AppUser> registry = new LinkedHashMap<>();
        registry.put("admin", new AppUser("admin", encoded, "ADMIN", null));
        registry.put("hr", new AppUser("hr", encoded, "HR", null));
        registry.put("karyawan", new AppUser("karyawan", encoded, "KARYAWAN", 1));
        this.users = Map.copyOf(registry);
    }

    @Override
    public AppUser loadUserByUsername(String username) {
        return find(username).orElseThrow(
                () -> new UsernameNotFoundException("Pengguna tidak dikenal: " + username));
    }

    public Optional<AppUser> find(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public String rawPassword() {
        return rawPassword;
    }
}
