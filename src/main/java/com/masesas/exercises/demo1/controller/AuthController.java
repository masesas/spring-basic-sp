package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.AuthResponse;
import com.masesas.exercises.demo1.dto.BaseApiResponse;
import com.masesas.exercises.demo1.dto.CustomerRegisterRequest;
import com.masesas.exercises.demo1.dto.CustomerResponse;
import com.masesas.exercises.demo1.dto.LoginRequest;
import com.masesas.exercises.demo1.dto.TokenResponse;
import com.masesas.exercises.demo1.exception.UnauthorizedException;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import com.masesas.exercises.demo1.security.LoginAttemptService;
import com.masesas.exercises.demo1.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@Tag(name = "Auth", description = "Pendaftaran, login, dan penerbitan token. Semua endpoint di sini terbuka tanpa token.")
public class AuthController {

    private static final String TIPE_TOKEN = "Bearer";
    private static final String PESAN_TERKUNCI = "Akun terkunci sementara karena terlalu banyak percobaan gagal";
    private static final String KODE_TERKUNCI = "ACCOUNT_LOCKED";
    private static final String PESAN_LOGIN_BERHASIL = "Login berhasil, token diterbitkan";

    private final AppUserDetailsService userDetailsService;
    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;
    private final JwtService jwtService;
    private final Clock clock;

    @PostMapping("/karyawan/login")
    @Operation(
            summary = "Login akun karyawan",
            description = "Menukar username dan password dengan JWT yang berlaku 15 menit. "
                    + "Setelah lima kali gagal berturut-turut, akun terkunci sementara.")
    @ApiResponse(responseCode = "200", description = PESAN_LOGIN_BERHASIL)
    @ApiResponse(responseCode = "423", description = "Akun terkunci sementara karena terlalu banyak percobaan gagal",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = BaseApiResponse.class)))
    public ResponseEntity<BaseApiResponse<AuthResponse>> loginKaryawan(@Valid @RequestBody LoginRequest request) {
        return login(userDetailsService.findKaryawan(request.getUsername()), request);
    }

    @PostMapping(path = "/karyawan/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(
            summary = "Terbitkan token karyawan (OAuth2 password flow)",
            description = "Endpoint yang dipanggil tombol Authorize di halaman ini untuk skema "
                    + "karyawanAuth. Menerima form-urlencoded dan membalas dalam bentuk OAuth2 "
                    + "sehingga tokennya bisa dipasang otomatis. Badan response endpoint ini "
                    + "sengaja tidak dibungkus BaseApiResponse karena spesifikasi OAuth2 "
                    + "mewajibkan access_token berada di akar JSON. Pemeriksaan kredensial dan "
                    + "penguncian akunnya sama persis dengan /api/auth/karyawan/login.")
    @ApiResponse(responseCode = "200", description = "Token diterbitkan",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "423", description = "Akun terkunci sementara",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = BaseApiResponse.class)))
    public ResponseEntity<Object> tokenKaryawan(@Valid LoginRequest request) {
        return token(userDetailsService.findKaryawan(request.getUsername()), request);
    }

    @PostMapping(path = "/customer/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(
            summary = "Terbitkan token customer (OAuth2 password flow)",
            description = "Padanan /api/auth/karyawan/token untuk skema customerAuth.")
    @ApiResponse(responseCode = "200", description = "Token diterbitkan",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "423", description = "Akun terkunci sementara",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = BaseApiResponse.class)))
    public ResponseEntity<Object> tokenCustomer(@Valid LoginRequest request) {
        return token(userDetailsService.findCustomer(request.getUsername()), request);
    }

    @PostMapping("/customer/register")
    @Operation(
            summary = "Daftarkan akun customer baru",
            description = "Email harus belum terpakai. Password disimpan sebagai hash bcrypt.")
    @ApiResponse(responseCode = "201", description = "Akun customer dibuat")
    public ResponseEntity<BaseApiResponse<CustomerResponse>> register(
            @Valid @RequestBody CustomerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseApiResponse.created("Akun customer dibuat", customerService.register(request)));
    }

    @PostMapping("/customer/login")
    @Operation(
            summary = "Login akun customer",
            description = "Sama dengan login karyawan, tapi memeriksa tabel customer.")
    @ApiResponse(responseCode = "200", description = PESAN_LOGIN_BERHASIL)
    @ApiResponse(responseCode = "423", description = "Akun terkunci sementara",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = BaseApiResponse.class)))
    public ResponseEntity<BaseApiResponse<AuthResponse>> loginCustomer(@Valid @RequestBody LoginRequest request) {
        return login(userDetailsService.findCustomer(request.getUsername()), request);
    }

    private ResponseEntity<BaseApiResponse<AuthResponse>> login(Optional<AppUser> found, LoginRequest request) {
        if (loginAttempts.isLocked(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.LOCKED).body(terkunci());
        }

        AppUser user = periksaKredensial(found, request);
        AuthResponse response = new AuthResponse(terbitkan(user), user.getTipe(), user.getRoles());
        return ResponseEntity.ok(BaseApiResponse.ok(PESAN_LOGIN_BERHASIL, response));
    }

    private ResponseEntity<Object> token(Optional<AppUser> found, LoginRequest request) {
        if (loginAttempts.isLocked(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.LOCKED).body(terkunci());
        }

        AppUser user = periksaKredensial(found, request);
        return ResponseEntity.ok(new TokenResponse(terbitkan(user), TIPE_TOKEN, jwtService.ttlSeconds()));
    }

    private <T> BaseApiResponse<T> terkunci() {
        return BaseApiResponse.error(HttpStatus.LOCKED, PESAN_TERKUNCI, KODE_TERKUNCI);
    }

    private AppUser periksaKredensial(Optional<AppUser> found, LoginRequest request) {
        if (found.isEmpty() || !passwordEncoder.matches(request.getPassword(), found.get().getPassword())) {
            loginAttempts.recordFailure(request.getUsername());
            throw new UnauthorizedException("Username atau password salah");
        }

        loginAttempts.reset(request.getUsername());
        return found.get();
    }

    private String terbitkan(AppUser user) {
        return jwtService.issue(user, Instant.now(clock));
    }

    // this is change from feat/test
}
