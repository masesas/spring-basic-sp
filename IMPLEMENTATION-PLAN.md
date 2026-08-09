# Implementation Plan — OWASP Top 10:2021

Rencana penerapan 10 kategori OWASP Top 10:2021 pada proyek `demo1` (Spring Boot 4.1, Java 21).

Dokumen ini menjelaskan **cara membangunnya**. Untuk penjelasan tiap kategori dan peta lokasi kode, lihat [TOP10-OWASP.md](./TOP10-OWASP.md).

---

## 1. Keputusan yang sudah disepakati

| Hal | Keputusan |
|---|---|
| Versi acuan | OWASP Top 10:**2021** |
| Bentuk | Tiap kategori punya **versi rentan** dan **versi aman**, plus test yang membuktikan eksploit berhasil di versi rentan dan gagal di versi aman |
| Autentikasi | JWT ditulis manual dengan **jjwt** (bukan `oauth2-resource-server`), tetap STATELESS |
| Serializer JWT | **`jjwt-gson`** — proyek memakai Jackson 3 (`tools.jackson`), sedangkan `jjwt-jackson` butuh Jackson 2 |
| SSRF | Fitur **ambil foto profil dari URL** |
| Command Injection | **Dilewati** — tidak ada alasan wajar HR API memanggil shell |
| Jumlah | **22 implementasi** (A03 dipecah jadi SQL dan XSS) |

---

## 2. Dependency baru

### `pom.xml` — dependencies

| Artifact | Scope | Untuk |
|---|---|---|
| `org.springframework.boot:spring-boot-starter-validation` | compile | A03 |
| `io.jsonwebtoken:jjwt-api` | compile | A07 |
| `io.jsonwebtoken:jjwt-impl` | runtime | A07 |
| `io.jsonwebtoken:jjwt-gson` | runtime | A07 |
| `com.bucket4j:bucket4j-core` | compile | A04 |

### `pom.xml` — build plugins

| Plugin | Untuk |
|---|---|
| `org.owasp:dependency-check-maven` | A06 — gagalkan build bila CVSS ≥ 7 |
| `org.apache.maven.plugins:maven-enforcer-plugin` | A06 — larang versi dinamis dan dependency SNAPSHOT |

> **Verifikasi Fase 1:** pastikan `jjwt-gson` benar-benar bebas dari Jackson 2. Bila ternyata masih menarik Jackson 2, hentikan dan laporkan sebelum lanjut.

---

## 3. Struktur folder

```
src/main/java/com/masesas/exercises/demo1/
├── security/                     infrastruktur auth
│   ├── JwtService.java           terbitkan & validasi token
│   ├── JwtAuthFilter.java        OncePerRequestFilter
│   └── AppUserDetailsService.java
├── owasp/
│   ├── vuln/                     SEMUA versi rentan
│   │   ├── A01VulnController.java
│   │   ├── A02VulnController.java
│   │   └── ...
│   └── safe/                     SEMUA versi aman
│       ├── A01SafeController.java
│       └── ...
└── config/SecurityConfig.java    diubah

src/test/java/com/masesas/exercises/demo1/owasp/
├── A01AccessControlTest.java
├── A02CryptoTest.java
└── ...
```

**Aturan:**

- Endpoint rentan selalu di bawah prefix `/api/vuln/**`, endpoint aman di `/api/safe/**`.
- Seluruh package `owasp.vuln` diberi `@Profile("owasp-demo")` sehingga **tidak pernah aktif** kecuali profil itu dinyalakan secara sengaja.
- **Tidak ada komentar di kode implementasi.** Penjelasan hanya hidup di `TOP10-OWASP.md`.
- Tiga kategori tidak berbentuk endpoint (A05, A06, A08) — pasangannya berupa perubahan konfigurasi, bukan controller. Dijelaskan per kategori di bawah.

---

## 4. Fase pengerjaan

Empat fase berurutan. Fase 1 wajib selesai lebih dulu karena A01 mustahil diuji tanpa konsep "siapa yang login".

---

### Fase 1 — Fondasi

#### A07 — Identification & Authentication Failures

| | Isi |
|---|---|
| Rentan 1 | `POST /api/vuln/login` — membandingkan password sebagai teks biasa, mengembalikan token tanpa masa berlaku |
| Rentan 2 | Percobaan login gagal tak terbatas |
| Aman 1 | `POST /api/safe/login` — verifikasi lewat `PasswordEncoder`, terbitkan JWT dengan masa berlaku 15 menit |
| Aman 2 | Kunci akun 15 menit setelah 5 kali gagal, penghitung disimpan di Redis |
| File | `security/JwtService`, `security/JwtAuthFilter`, `security/AppUserDetailsService`, `security/LoginAttemptService`, `owasp/vuln/A07VulnController`, `owasp/safe/A07SafeController` |
| Test | Token kedaluwarsa ditolak; percobaan ke-6 dibalas 423 Locked |

User disimpan **in-memory** (`InMemoryUserDetailsManager`) dengan tiga akun: `admin`, `hr`, `karyawan`. Tidak menambah tabel baru.

#### A01 — Broken Access Control

| | Isi |
|---|---|
| Rentan 1 | `GET /api/vuln/payroll/{idKaryawan}` — siapa pun bisa baca slip gaji siapa pun |
| Rentan 2 | `DELETE /api/vuln/karyawan/{id}` — tanpa cek peran |
| Aman 1 | `@PreAuthorize("hasRole('HR')")` pada operasi tulis payroll |
| Aman 2 | Anti-IDOR: `@PreAuthorize("hasRole('HR') or #idKaryawan == authentication.principal.idKaryawan")` |
| File | `config/SecurityConfig` (aturan per-endpoint + `@EnableMethodSecurity`), `owasp/vuln/A01VulnController`, `owasp/safe/A01SafeController` |
| Test | Peran `karyawan` membaca slip milik orang lain → 200 di versi rentan, 403 di versi aman |

`SecurityConfig` berubah dari `anyRequest().permitAll()` menjadi aturan per-endpoint. Ini **breaking change**: seluruh endpoint lama akan meminta token.

---

### Fase 2 — Validasi input

#### A03 — Injection (SQL)

| | Isi |
|---|---|
| Rentan 1 | `GET /api/vuln/karyawan/search?nama=` — SQL dirangkai dengan penggabungan string |
| Rentan 2 | `GET /api/vuln/karyawan/sort?by=` — kolom `ORDER BY` langsung dari input user |
| Aman 1 | Bind parameter `?` + `@Valid` pada seluruh `@RequestBody` |
| Aman 2 | Allowlist kolom sort lewat `enum SortField` |
| File | `owasp/vuln/A03SqlVulnController`, `owasp/safe/A03SqlSafeController`, `owasp/safe/SortField`, seluruh DTO di `dto/` ditambah anotasi validasi |
| Test | Payload `' OR '1'='1` mengembalikan semua baris di versi rentan, 400 di versi aman |

Anotasi validasi yang dipasang: `@NotBlank` pada `nama`, `@Pattern(regexp="\\d{16}")` pada `nik`, `@Pattern(regexp="\\d{4}-\\d{2}")` pada `periode`, `@PositiveOrZero` pada nominal gaji. `GlobalExceptionHandler` ditambah penanganan `MethodArgumentNotValidException` dan `ConstraintViolationException` → 400.

#### A03 — Injection (XSS)

| | Isi |
|---|---|
| Rentan 1 | `POST /api/vuln/karyawan` — menyimpan `<script>` di kolom `nama` apa adanya (stored XSS) |
| Rentan 2 | Input user masuk ke log tanpa disaring (log injection) |
| Aman 1 | Tolak tag HTML pada field teks bebas + header `X-Content-Type-Options: nosniff` |
| Aman 2 | Buang `\r` dan `\n` dari input sebelum masuk log |
| File | `owasp/vuln/A03XssVulnController`, `owasp/safe/A03XssSafeController`, `owasp/safe/InputSanitizer` |
| Test | Payload `<script>alert(1)</script>` tersimpan utuh di versi rentan, ditolak di versi aman |

Proyek ini REST/JSON murni tanpa template engine, jadi yang relevan hanya **stored XSS** — payload disimpan lalu meledak di frontend yang menampilkannya.

#### A04 — Insecure Design

| | Isi |
|---|---|
| Rentan 1 | `POST /api/vuln/login` tanpa pembatasan laju |
| Rentan 2 | Slip gaji berstatus `APPROVED` masih bisa diubah, periode masa depan diterima |
| Aman 1 | Bucket4j — 10 permintaan/menit per IP pada endpoint login dan pencarian |
| Aman 2 | Aturan bisnis di layer domain: `APPROVED` tidak bisa direvisi, periode masa depan ditolak |
| File | `owasp/safe/RateLimitFilter`, `entity/PayrollKaryawan` (tambah field `status`), `service/impl/PayrollServiceImpl` |
| Test | Permintaan ke-11 dalam satu menit dibalas 429; revisi slip `APPROVED` dibalas 422 |

---

### Fase 3 — Konfigurasi dan data

#### A02 — Cryptographic Failures

| | Isi |
|---|---|
| Rentan 1 | `GET /api/vuln/karyawan/{id}/detail` — `nik` dan `npwp` dikembalikan apa adanya |
| Rentan 2 | Password di-hash dengan MD5 |
| Aman 1 | `AttributeConverter` + `@Convert` — enkripsi AES-GCM kolom `nik` dan `npwp`, kunci dari environment |
| Aman 2 | `DelegatingPasswordEncoder` dengan bcrypt strength 12; response menampilkan `nik` tersamar (`************1234`) |
| File | `owasp/safe/CryptoConverter`, `entity/DetailKaryawan` (tambah `@Convert`), `config/SecurityConfig` (ganti encoder), `owasp/vuln/A02VulnController`, `owasp/safe/A02SafeController` |
| Test | Kolom `nik` di DB tidak sama dengan nilai aslinya; response tidak pernah memuat NIK lengkap |

Enkripsi kolom `nik`/`npwp` mengubah isi kolom yang ada. Data lama perlu dimigrasi atau tabel di-seed ulang — dikerjakan lewat `seeder.sql`.

#### A05 — Security Misconfiguration

Kategori ini **tidak berbentuk endpoint**. Pasangannya adalah kondisi konfigurasi sebelum dan sesudah.

| | Isi |
|---|---|
| Rentan (kondisi sekarang) | Kredensial hardcoded, `show-sql=true`, devtools di scope runtime, tanpa security header |
| Aman 1 | Kredensial pindah ke `.env` via `spring.config.import=optional:file:.env[.properties]`; `.env` masuk `.gitignore`, `.env.example` ikut di-commit |
| Aman 2 | Security header: HSTS, CSP, `frameOptions.deny()`, `nosniff`; CORS allowlist eksplisit; `server.error.include-stacktrace=never` |
| File | `.env`, `.env.example`, `.gitignore`, `application.properties`, `config/SecurityConfig` |
| Test | Response memuat seluruh header keamanan; `application.properties` tidak memuat satu pun nilai kredensial literal |

Devtools dipindah ke profil `dev` saja. `spring.jpa.show-sql` dimatikan.

#### A08 — Software & Data Integrity Failures

| | Isi |
|---|---|
| Rentan 1 | Dua permintaan bersamaan mengubah slip gaji yang sama — perubahan pertama hilang |
| Rentan 2 | Deserialisasi Redis tanpa allowlist tipe |
| Aman 1 | `@Version` pada `PayrollKaryawan` (optimistic locking) → 409 saat bentrok |
| Aman 2 | Pertahankan dan uji `BasicPolymorphicTypeValidator` yang sudah ada di `RedisConfig` |
| File | `entity/PayrollKaryawan`, `config/RedisConfig`, `owasp/vuln/A08VulnController`, `owasp/safe/A08SafeController` |
| Test | Update bersamaan → yang kedua dibalas 409; deserialisasi kelas di luar allowlist ditolak |

Butuh kolom baru `version` pada tabel `masesas.payroll_karyawan`.

---

### Fase 4 — Observabilitas dan jaringan

#### A09 — Security Logging & Monitoring Failures

| | Isi |
|---|---|
| Rentan 1 | Operasi tulis payroll tidak meninggalkan jejak sama sekali |
| Rentan 2 | `Karyawan2Service:64` membuang stacktrace (`log.error("...", e.getMessage())`) |
| Aman 1 | Aspect `@Around` pada operasi tulis payroll → log terstruktur berisi aktor, aksi, waktu, IP |
| Aman 2 | Log event login sukses/gagal + correlation ID lewat `OncePerRequestFilter` (MDC) |
| File | `owasp/safe/AuditAspect`, `owasp/safe/CorrelationIdFilter`, `service/Karyawan2Service` (perbaiki logging) |
| Test | Perubahan payroll menghasilkan satu baris audit; satu permintaan memakai satu correlation ID yang sama |

Butuh `spring-boot-starter-aop` (sudah tersedia transitif lewat `spring-boot-starter-data-jpa`, akan diverifikasi).

#### A10 — Server-Side Request Forgery

| | Isi |
|---|---|
| Rentan 1 | `POST /api/vuln/karyawan/{id}/foto` — mengambil URL apa pun dari user |
| Rentan 2 | Redirect diikuti otomatis, tanpa timeout |
| Aman 1 | Hanya skema `https`; resolve DNS lalu tolak IP privat, loopback, dan link-local |
| Aman 2 | Redirect tidak diikuti, timeout 3 detik, batas ukuran 2 MB, `Content-Type` wajib `image/*` |
| File | `config/ClientConfig` (timeout), `owasp/safe/UrlGuard`, `owasp/vuln/A10VulnController`, `owasp/safe/A10SafeController` |
| Test | `http://127.0.0.1:6379` dan `http://169.254.169.254/latest/meta-data/` berhasil ditembak di versi rentan, dibalas 400 di versi aman |

#### A06 — Vulnerable & Outdated Components

Kategori ini **tidak berbentuk endpoint**. Pasangannya adalah kondisi `pom.xml` sebelum dan sesudah.

| | Isi |
|---|---|
| Rentan (kondisi sekarang) | Tidak ada pemindaian kerentanan sama sekali |
| Aman 1 | `dependency-check-maven` terikat ke fase `verify`, `failBuildOnCVSS=7` |
| Aman 2 | `maven-enforcer-plugin` — larang versi dinamis dan dependency SNAPSHOT |
| File | `pom.xml` |
| Test | Build gagal bila ada kerentanan CVSS ≥ 7 |

> Jalankan **paling akhir**. Pemindaian pertama mengunduh basis data NVD dan bisa memakan beberapa menit.

---

## 5. Testing

- Setiap kategori punya satu kelas test di `src/test/java/.../owasp/`.
- Pola tiap test: jalankan serangan yang sama ke endpoint rentan dan ke endpoint aman — **harus berhasil di yang rentan, gagal di yang aman**.
- Test dijalankan dengan profil `owasp-demo` aktif supaya package `vuln` ikut termuat.
- Target: seluruh test lolos, `mvn -q verify` hijau di akhir setiap fase.

---

## 6. Di luar cakupan

- **Command Injection** — tidak ada permukaan serangan wajar di HR API.
- **Reflected XSS** — proyek ini REST/JSON murni tanpa render HTML.
- Tabel `user`/`role` di database — akun disimpan in-memory.
- Refresh token dan rotasinya — hanya access token 15 menit.
- Integrasi SIEM, alerting, dan dashboard — A09 berhenti di log terstruktur.

## 7. Batasan yang diketahui

- Package `owasp.vuln` berisi kerentanan **nyata**. Dipagari `@Profile("owasp-demo")` dan prefix `/api/vuln`, tetapi repo ini publik — jangan pernah men-deploy dengan profil tersebut menyala.
- Fase 1 memutus kompatibilitas: seluruh endpoint lama akan meminta token JWT.
- Enkripsi `nik`/`npwp` mengubah data kolom yang sudah ada.
- `dependency-check` butuh koneksi internet dan unduhan NVD pada eksekusi pertama.

---

## 8. Satu hal yang belum diputuskan

**Kredensial yang sudah bocor publik.** Repo `github.com/masesas/spring-basic-sp` bersifat **PUBLIC** dan `application.properties` di dalamnya memuat password PostgreSQL serta Redis sejak commit pertama.

Menghapus git history **tidak** memperbaiki kebocoran — password harus **dirotasi**. Karena `binar_finance` adalah database bersama beberapa peserta, rotasi perlu koordinasi dengan pengelolanya.

Tiga pilihan:

| | Pilihan | Konsekuensi |
|---|---|---|
| **A** | Rotasi dulu, baru `.env` + bersihkan history | Paling benar. **Direkomendasikan.** |
| **B** | Kerjakan `.env` + bersihkan history sekarang, rotasi menyusul | Rapi secara struktur, tapi password lama tetap bocor |
| **C** | Tunda, langsung mulai Fase 1 | Kebocoran dibiarkan lebih lama |

Fase 3 (A05) tidak bisa diselesaikan sebelum ini diputuskan.
