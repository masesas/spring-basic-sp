package com.masesas.exercises.demo1.owasp.safe;

import com.masesas.exercises.demo1.dto.LoginRequest;
import com.masesas.exercises.demo1.dto.LoginResponse;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import com.masesas.exercises.demo1.security.LoginAttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/safe")
@RequiredArgsConstructor
public class A07SafeController {

    private final AppUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;
    private final JwtService jwtService;
    private final AuditLogger auditLogger;
    private final Clock clock;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        if (loginAttempts.isLocked(request.username())) {
            auditLogger.catat("auth.login", request.username(), "DITOLAK:TERKUNCI");
            return ResponseEntity.status(HttpStatus.LOCKED).build();
        }

        Optional<AppUser> found = userDetailsService.find(request.username());
        if (found.isEmpty() || !passwordEncoder.matches(request.password(), found.get().getPassword())) {
            loginAttempts.recordFailure(request.username());
            auditLogger.catat("auth.login", request.username(), "GAGAL:KREDENSIAL");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        loginAttempts.reset(request.username());
        auditLogger.catat("auth.login", request.username(), "BERHASIL");
        AppUser user = found.get();
        String token = jwtService.issue(user, Instant.now(clock));
        return ResponseEntity.ok(new LoginResponse(token, user.role()));
    }
}
