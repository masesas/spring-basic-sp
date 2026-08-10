package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.AuthResponse;
import com.masesas.exercises.demo1.dto.CustomerRegisterRequest;
import com.masesas.exercises.demo1.dto.CustomerResponse;
import com.masesas.exercises.demo1.dto.LoginRequest;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import com.masesas.exercises.demo1.security.LoginAttemptService;
import com.masesas.exercises.demo1.service.CustomerService;
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
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AppUserDetailsService userDetailsService;
    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;
    private final JwtService jwtService;
    private final Clock clock;

    @PostMapping("/karyawan/login")
    public ResponseEntity<AuthResponse> loginKaryawan(@Valid @RequestBody LoginRequest request) {
        return login(userDetailsService.findKaryawan(request.getUsername()), request);
    }

    @PostMapping("/customer/register")
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody CustomerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.register(request));
    }

    @PostMapping("/customer/login")
    public ResponseEntity<AuthResponse> loginCustomer(@Valid @RequestBody LoginRequest request) {
        return login(userDetailsService.findCustomer(request.getUsername()), request);
    }

    private ResponseEntity<AuthResponse> login(Optional<AppUser> found, LoginRequest request) {
        if (loginAttempts.isLocked(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.LOCKED).build();
        }

        if (found.isEmpty() || !passwordEncoder.matches(request.getPassword(), found.get().getPassword())) {
            loginAttempts.recordFailure(request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        loginAttempts.reset(request.getUsername());
        AppUser user = found.get();
        String token = jwtService.issue(user, Instant.now(clock));
        return ResponseEntity.ok(new AuthResponse(token, user.getTipe(), user.getRoles()));
    }
}
