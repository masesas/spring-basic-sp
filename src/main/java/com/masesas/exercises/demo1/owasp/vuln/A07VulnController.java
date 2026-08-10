package com.masesas.exercises.demo1.owasp.vuln;

import com.masesas.exercises.demo1.dto.LoginRequest;
import com.masesas.exercises.demo1.dto.LoginResponse;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/vuln")
@Profile("owasp-demo")
@RequiredArgsConstructor
public class A07VulnController {

    private final AppUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Optional<AppUser> found = userDetailsService.find(request.getUsername());
        if (found.isEmpty() || !passwordEncoder.matches(request.getPassword(), found.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AppUser user = found.get();
        String token = jwtService.issueWithoutExpiry(user, Instant.now(clock));
        return ResponseEntity.ok(new LoginResponse(token, String.join(",", user.getRoles())));
    }
}
