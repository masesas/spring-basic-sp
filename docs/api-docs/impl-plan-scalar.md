# Implementation Plan — Dokumentasi API dengan Scalar

Status: Selesai. Semua tahap dieksekusi dan diverifikasi, kecuali satu item yang
terhalang lingkungan — lihat Risiko.

Menambahkan dokumentasi API interaktif memakai Scalar di endpoint `/docs`, tanpa Swagger UI,
dengan pengisian bearer token otomatis lewat OAuth2 password flow.

---

## Keputusan yang Sudah Ditetapkan

| # | Keputusan | Nilai |
|---|---|---|
| 1 | Path UI | `/docs` — bukan `/scalar`, bukan `/` |
| 1 | Akses | publik, tanpa token |
| 2 | Cakupan profile | semua profile → konfigurasi di `application.properties`, bukan per-profile |
| 3 | Kedalaman anotasi | lengkap: request, response, metadata, kode error, contoh nilai |
| 4 | Auth di UI | otomatis lewat OAuth2 password flow, user tidak menempel token manual |

---

## Rancangan

### 1. Dependency

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-scalar</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

dengan `<springdoc.version>3.1.0</springdoc.version>` di `<properties>`.

Versi ditulis eksplisit karena `spring-boot-dependencies:4.1.0` tidak mengelola springdoc, dan
`maven-enforcer-plugin` di proyek ini mengaktifkan `banDynamicVersions`.

Fakta yang sudah diverifikasi dari POM dan isi jar:

- springdoc 3.1.0 dibangun dengan parent `spring-boot-starter-parent:4.1.0` — sama persis dengan proyek ini.
- Transitif: `springdoc-openapi-starter-webmvc-api:3.1.0` dan `com.scalar.maven:scalar-webmvc:0.5.55`.
- **Webjar `swagger-ui` tidak ikut** — tidak ada Swagger UI di classpath.
- `scalar-webmvc` mendeklarasikan Spring Boot 3.5.7, tapi semuanya `provided`/`optional` sehingga
  tidak bocor ke classpath runtime.
- springdoc memasang `AutoConfigurationImportFilter` (`ScalarDisableAutoConfiguration`) yang
  mematikan `ScalarWebMvcAutoConfiguration` bawaan Scalar. Tidak ada dua controller yang
  memperebutkan path yang sama.
- Aset UI di-host sendiri (`scalar.js` 3,5 MB di dalam jar), bukan dari CDN.

Anotasi `io.swagger.v3.oas.annotations` (swagger-core 2.2.52) tetap dipakai sebagai generator
spesifikasi. Itu library pembangkit spec, bukan Swagger UI, dan tidak punya pengganti di ekosistem Spring.

### 2. Endpoint yang muncul

| Path | Isi |
|---|---|
| `GET /docs` | halaman UI Scalar (HTML) |
| `GET /docs/scalar.js` | bundle JS, same-origin, dari webjar |
| `GET /docs/openapi` | spesifikasi OpenAPI 3.1 (JSON) |
| `GET /docs/openapi.yaml` | spesifikasi versi YAML |

Semua di bawah satu prefix `/docs`, sehingga aturan keamanan cukup satu pola.

Path UI berasal dari `@RequestMapping("${scalar.path:/scalar}")` di
`org.springdoc.webmvc.scalar.ScalarWebMvcController`, dan `/docs/scalar.js` dari method
`getScalarJs()` yang diwarisi dari `AbstractScalarController` dengan
`@GetMapping({"/scalar.js", "scalar.js"})`.

### 3. Konfigurasi — `application.properties`

Ditempatkan di `application.properties` sesuai keputusan #2, karena tidak ada nilai yang
berbeda antar lingkungan.

```properties
# ===== Dokumentasi API (Scalar) =====
# UI di /docs, spesifikasi di /docs/openapi — satu prefix supaya aturan
# keamanannya cukup satu pola.
springdoc.api-docs.path=/docs/openapi
scalar.path=/docs
scalar.page-title=demo1 API

# Default Scalar mengirim telemetri ke server mereka dan mengunduh font dari
# fonts.scalar.com. Keduanya dimatikan supaya halaman ini tidak melakukan
# permintaan keluar sama sekali.
scalar.telemetry=false
scalar.with-default-fonts=false

# Token hasil login tersimpan di browser, tidak hilang saat halaman dimuat ulang.
scalar.persist-auth=true
```

Yang sengaja **tidak** diisi: `scalar.proxy-url`. Kalau diisi, permintaan "Try it" —
berikut header `Authorization` — akan diteruskan lewat `proxy.scalar.com`.

Default yang perlu ditimpa ini dibaca langsung dari bytecode constructor `ScalarProperties`:
`telemetry=true`, `withDefaultFonts=true`, `path=/scalar`, `enabled=true`.

### 4. Keamanan — `SecurityConfig`

Dua perubahan.

**4a. Filter chain terpisah untuk `/docs`.**

Chain utama memasang `default-src 'none'` (`SecurityConfig.java:57`). Halaman Scalar butuh satu
`<script src>` same-origin **dan satu `<script>` inline** (`Scalar.createApiReference(...)`),
jadi CSP itu membuat halaman render kosong. Melonggarkan CSP global bukan pilihan — itu
menurunkan proteksi seluruh API demi satu halaman.

Chain `@Order(1)` dengan `securityMatcher("/docs", "/docs/**")`:

- CSP: `default-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'`
- `permitAll()`
- `csrf` disable, `sessionCreationPolicy(STATELESS)`
- tanpa `JwtAuthFilter` — halaman dokumentasi tidak butuh autentikasi

Chain utama diberi `@Order(2)` dan isinya tidak berubah.

**4b. `/docs/**` ditambahkan ke `WHITELIST_ENDPOINTS`.**

Secara keamanan ini tidak berpengaruh — request `/docs/**` sudah ditangani chain `@Order(1)`
dan tidak pernah sampai ke chain utama. Tujuannya `RoleMapService.publik()`, yang membaca
konstanta yang sama, tetap melaporkan `/docs` sebagai endpoint publik di `/api/rolemap`.

### 5. Bearer token otomatis

Mekanisme yang dipakai Scalar (terverifikasi dari isi `scalar.js`):

1. User memilih security scheme di panel Auth, mengisi username dan password.
2. Scalar `POST` ke `tokenUrl` dengan `Content-Type: application/x-www-form-urlencoded`,
   body `grant_type=password&username=...&password=...`.
3. Scalar membaca token dari response JSON pada field `access_token`.
4. Token di-attach sebagai `Authorization: Bearer ...` ke setiap request "Try it" berikutnya.
5. Dengan `scalar.persist-auth=true`, token bertahan setelah halaman dimuat ulang.

Endpoint login yang ada membalas `{token, tipe, roles}` dan menerima JSON, jadi tidak cocok.
Ditambahkan dua endpoint token di `AuthController` yang memakai ulang method `login()` privat
yang sudah ada:

| Endpoint | Untuk |
|---|---|
| `POST /api/auth/karyawan/token` | scheme `karyawanAuth` |
| `POST /api/auth/customer/token` | scheme `customerAuth` |

Keduanya `consumes = APPLICATION_FORM_URLENCODED_VALUE`, membalas DTO baru `TokenResponse`:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
}
```

dengan `@JsonProperty("access_token")`, `@JsonProperty("token_type")`, `@JsonProperty("expires_in")`
supaya nama field di JSON sesuai OAuth2, tanpa mengubah konvensi penamaan Java.

Response dipakai apa adanya oleh Scalar, jadi ekstensi `x-tokenName` tidak diperlukan.

`token_type` selalu `Bearer`. `expires_in` = `app.security.jwt-ttl-minutes` × 60.

Dua scheme terpisah dipilih daripada satu endpoint dengan parameter `scope`, karena memetakan
1:1 ke dua tipe pengguna yang memang sudah terpisah di `AppUserDetailsService`, dan user tinggal
memilih scheme yang sesuai perannya di panel Auth.

Endpoint baru ini **tidak menambah permukaan serangan**: kredensial, pemeriksaan password, dan
penguncian `LoginAttemptService` persis sama dengan endpoint login yang sudah ada. Yang berbeda
hanya content-type dan bentuk response. `/api/auth/**` sudah ada di `WHITELIST_ENDPOINTS`.

### 6. Anotasi OpenAPI

**6a. `config/OpenApiConfig.java`** — kelas biasa, tanpa komentar, sesuai aturan repo:

- `@OpenAPIDefinition` — `info` (judul, deskripsi, versi, kontak), `servers`
- `@SecurityScheme(name = "karyawanAuth", type = OAUTH2, flows = @OAuthFlows(password = @OAuthFlow(tokenUrl = "/api/auth/karyawan/token")))`
- `@SecurityScheme(name = "customerAuth", ...)` dengan `tokenUrl = "/api/auth/customer/token"`

**6b. `dto/ApiErrorResponse.java`** — DTO baru untuk skema error.

`GlobalExceptionHandler` membalas `Map<String, Object>` berisi `timestamp`, `status`, `error`,
`message`. springdoc tidak bisa menurunkan skema dari `Map`, jadi bentuknya ditulis sekali di
DTO ini dan dirujuk semua `@ApiResponse` error. DTO ini murni untuk dokumentasi — handler tidak diubah.

**6c. Per controller** — `@Tag(name, description)` di 9 controller:
`Auth`, `Customer`, `DummyJson`, `Karyawan`, `Karyawan2`, `KaryawanSp`, `KaryawanStatistik`,
`Payroll`, `RoleMap`.

**6d. Per endpoint** — sekitar 40 endpoint:

- `@Operation(summary, description)`
- `@ApiResponses` — kode sukses, plus yang relevan dari `GlobalExceptionHandler`:
  `400` (validasi), `401` (token/kredensial), `403` (peran kurang), `404` (tidak ditemukan),
  `409` (konflik/optimistic lock), `423` (akun terkunci, khusus login), `500`
- `@Parameter(description, example)` untuk setiap `@PathVariable` dan `@RequestParam`
- `@SecurityRequirement(name = "karyawanAuth")` di endpoint yang butuh peran karyawan,
  `customerAuth` di `/api/customer/me`; endpoint di `WHITELIST_ENDPOINTS` tidak diberi requirement

**6e. Per DTO** — 25 DTO di package `dto`: `@Schema(description, example)` per field.
`requiredMode` tidak ditulis manual — springdoc menurunkannya dari `@NotNull`/`@NotBlank`
yang sudah terpasang.

`@Schema` adalah anotasi, bukan komentar, jadi aturan "tanpa komentar di `.java`" tetap terpenuhi.

---

## Checklist Implementasi

Urutan ini dipilih supaya ada titik verifikasi sebelum pekerjaan anotasi yang panjang dimulai.

**Tahap 1 — dependency dan konfigurasi** — selesai

1. [x] Tambahkan `<springdoc.version>3.1.0</springdoc.version>` dan dependency di `pom.xml`
2. [x] Tambahkan blok konfigurasi Scalar ke `application.properties`
3. [x] `./mvnw clean compile` hijau
4. [x] `./mvnw test` — 158 test tetap hijau

**Tahap 2 — keamanan** — selesai

5. [x] Pecah `SecurityConfig` jadi dua chain: `@Order(1)` untuk `/docs`, `@Order(2)` chain utama
6. [x] CSP chain `/docs` sesuai rancangan 4a
7. [x] Tambahkan `/docs/**` ke `WHITELIST_ENDPOINTS` — lewat `DOCS_ENDPOINTS` yang digabung
       dengan `Stream.concat`, supaya satu daftar dipakai chain `/docs` sekaligus `RoleMapService`
8. [x] Tulis `DocsAccessTest`, jalankan — 5 test hijau
9. [x] Aplikasi dijalankan, keempat path diverifikasi lewat HTTP: `/docs` 200 HTML dengan
       konfigurasi benar terinjeksi (`telemetry:false`, `withDefaultFonts:false`,
       `persistAuth:true`, url `/docs/openapi`), `/docs/scalar.js` 200 (3,5 MB,
       `application/javascript`), `/docs/openapi` 200 dengan 32 path dan 32 skema,
       `/docs/openapi.yaml` 200. **Belum diverifikasi secara visual** — ekstensi browser
       tidak terhubung saat eksekusi, jadi eksekusi JS dan bersihnya console dari pelanggaran
       CSP masih perlu dilihat sendiri di browser.

**Tahap 3 — endpoint token** — selesai

10. [x] `dto/TokenResponse.java`
11. [x] `POST /api/auth/karyawan/token` dan `POST /api/auth/customer/token` di `AuthController`;
        method `login()` dipecah jadi `periksaKredensial()` + `terbitkan()` yang dipakai bersama
        oleh jalur login lama dan jalur token, supaya tidak ada logika kredensial yang terduplikasi;
        `JwtService.ttlSeconds()` ditambahkan sebagai sumber tunggal nilai `expires_in`
12. [x] Tulis `AuthTokenTest`, jalankan — 5 test hijau. Diverifikasi juga lewat HTTP sungguhan:
        response `{access_token, token_type: "Bearer", expires_in: 900}`, dan tokennya dipakai
        ke `GET /api/karyawan/all` membalas 200

**Tahap 4 — anotasi** — selesai

13. [x] `config/OpenApiConfig.java` dengan dua `@SecurityScheme`
14. [x] `dto/ApiErrorResponse.java`
15. [x] `@Tag` di 9 controller — diverifikasi lewat spesifikasi: 9 tag muncul
16. [x] `@Operation` + `@ApiResponse` + `@Parameter` + `@SecurityRequirement` di seluruh
        endpoint — diverifikasi: 0 operation tanpa `summary`
17. [x] `@Schema` di seluruh DTO dan model — 179 field di tiga package: `dto` (112),
        `model` (29), `dummyjsondto` (38). Diverifikasi lewat spesifikasi: 0 properti
        tanpa `description` di luar skema bawaan springdoc
18. [~] Panel Auth **belum dicoba dari browser** — ekstensi tidak terhubung. Yang sudah
        dibuktikan: spesifikasi memuat kedua scheme dengan `tokenUrl` yang benar, dan
        endpoint tokennya menjawab dengan benar terhadap permintaan yang bentuknya persis
        seperti yang dikirim Scalar (form-urlencoded `grant_type=password`, dibaca dari
        `access_token`). Yang belum: melihat sendiri tombolnya bekerja di halaman.

**Perubahan rancangan saat eksekusi.** Rencana awal menaruh kode error di tiap endpoint.
Ternyata `GlobalExceptionHandler` sudah `@RestControllerAdvice`, dan springdoc menerapkan
`@ApiResponse` dari sana ke seluruh operation. Kontraknya jadi ditulis sekali di tempat
error itu dibuat — sekitar 200 baris anotasi berulang hilang, dan `@ExceptionHandler` baru
otomatis terdokumentasi di semua endpoint. Rancangan 6b diubah mengikuti ini:
`GlobalExceptionHandler` **ikut dianotasi**, meski perilakunya tidak diubah sama sekali.

Cakupan anotasi juga melebar dari rencana: package `model` dan `dummyjsondto` ikut muncul
sebagai skema di `/docs/openapi`, jadi keduanya ikut dianotasi. Rencana awal hanya menyebut
package `dto`.

**Tahap 5 — penutup**

19. [x] `./mvnw test` — 172 test hijau
20. [~] `./mvnw verify` — `enforcer` lolos ketiga aturannya, `jacoco:check` lolos,
        172 test hijau. `dependency-check` **gagal dijalankan**, bukan menemukan kerentanan
        — lihat Risiko.
21. [x] `http/README.md`: `/docs` masuk matriks akses
22. [x] `docs/api-docs/README.md` ditulis

---

## Test yang Ditulis

**`config/DocsAccessTest.java`**

1. `GET /docs` tanpa token → 200, `Content-Type` HTML
2. `GET /docs/openapi` tanpa token → 200, JSON, memuat `openapi` dan `paths`
3. `GET /docs/scalar.js` tanpa token → 200
4. CSP di `/docs` memuat `script-src 'self' 'unsafe-inline'`
5. CSP di `/api/karyawan/all` tetap `default-src 'none'` — chain utama tidak ikut longgar
6. `/docs/openapi` memuat kedua security scheme `karyawanAuth` dan `customerAuth`
   — ditunda ke Tahap 4, karena schemenya baru ada setelah `OpenApiConfig` dibuat

**`controller/AuthTokenTest.java`**

7. `POST /api/auth/karyawan/token` form-urlencoded kredensial benar → 200,
   body punya `access_token`, `token_type=Bearer`, `expires_in`
8. Token dari langkah 7 dipakai sebagai `Authorization: Bearer` ke `/api/karyawan/all` → 200
9. Password salah → 401
10. Percobaan gagal berulang → 423, membuktikan `LoginAttemptService` tetap berlaku
11. `POST /api/auth/customer/token` kredensial customer benar → 200

**Regresi**

12. `RoleMapControllerTest` dan `RbacGuestTest` tetap hijau setelah endpoint springdoc ikut
    terdaftar di `RequestMappingHandlerMapping`

---

## Dampak ke Kode yang Sudah Ada

| Berkas | Perubahan |
|---|---|
| `pom.xml` | + 1 property, + 1 dependency |
| `application.properties` | + blok konfigurasi Scalar |
| `SecurityConfig.java` | dipecah jadi 2 filter chain, `WHITELIST_ENDPOINTS` bertambah |
| `AuthController.java` | + 2 endpoint token, + anotasi |
| 9 controller | + anotasi |
| 25 DTO | + `@Schema` |
| `dto/TokenResponse.java`, `dto/ApiErrorResponse.java`, `config/OpenApiConfig.java` | baru |
| `http/README.md`, `docs/api-docs/README.md` | dokumentasi |

`GlobalExceptionHandler` tidak diubah. Logika bisnis tidak diubah. Tidak ada service atau
repository yang tersentuh.

`/api/rolemap` akan bertambah entri karena `RoleMapService.semua()` mengenumerasi seluruh
`RequestMappingHandlerMapping`, dan controller springdoc terdaftar di sana. Test yang ada
memakai filter jsonPath, bukan hitungan, jadi kemungkinan besar tetap hijau — tapi itu harus
dibuktikan dengan menjalankan test, bukan diasumsikan.

---

## Risiko yang Belum Terverifikasi

Tiga hal ini tidak bisa dipastikan tanpa menjalankan build, dan harus dicek di tahap yang disebut.

1. **`dependency-check` masih terbuka — tapi bukan karena temuan keamanan.**
   `mvn verify` gagal di plugin ini, dan penyebabnya bukan kerentanan pada dependency baru:
   pemindaiannya tidak pernah sampai menganalisis dependency apa pun. Ia berhenti di tahap
   pembaruan data NVD:

   ```
   Engine.initializeAndUpdateDatabase -> doUpdates -> NvdApiDataSource.processApi
   UpdateException: Error updating the NVD Data
     caused by NullPointerException: Cannot read the array length because "bytes" is null
   NoDataException: No documents exist
   ```

   `NoDataException: No documents exist` berarti basis data NVD lokal kosong — belum pernah
   terunduh utuh di mesin ini. Dari stack trace terlihat kegagalannya terjadi di
   `initializeAndUpdateDatabase`, sebelum `analyzeDependencies` berjalan, sehingga daftar
   dependency proyek belum ikut terbaca sama sekali. Artinya kegagalan ini tidak bergantung
   pada dependency mana pun yang dideklarasikan, termasuk yang baru ditambahkan.

   Endpoint NVD sendiri bisa dihubungi (HTTP 200), jadi kemungkinan besar ini pembatasan laju
   untuk akses tanpa API key. Untuk menuntaskannya:

   ```bash
   ./mvnw verify -Dnvd.api.key=ISI_API_KEY_ANDA
   ```

   Sampai itu dijalankan, **belum ada bukti** bahwa swagger-core 2.2.52 dan scalar-core 0.5.55
   bebas dari CVE dengan CVSS >= 7. Kalau nanti ada temuan yang versinya tidak bisa dinaikkan,
   tambahkan entri beralasan ke `dependency-check-suppressions.xml`.

2. ~~**Serialisasi `Page`.**~~ **Selesai — cocok.** Dibandingkan langsung antara response
   sungguhan `GET /api/karyawan?size=2` dan skema `PagedModelKaryawanResponse`: keduanya
   `{content, page}` dengan `page` berisi `{number, size, totalElements, totalPages}`.
   `serialization-mode=via-dto` sudah tercermin benar di spesifikasi.

3. ~~**Kompatibilitas runtime `scalar-webmvc`.**~~ **Selesai — tidak ada masalah.** Aplikasi
   menyala normal dan keempat path `/docs` melayani permintaan dengan benar.

**Temuan tambahan saat eksekusi**

Spring Boot 4 memakai **Jackson 3**, sehingga `com.fasterxml.jackson.databind.ObjectMapper`
tidak lagi tersedia sebagai bean (kelasnya pindah ke `tools.jackson.databind`). Anotasi
`com.fasterxml.jackson.annotation.JsonProperty` tetap berlaku — dibuktikan oleh nama field
snake_case pada response `TokenResponse`. Test yang butuh membaca JSON memakai
`com.jayway.jsonpath.JsonPath`, bukan `ObjectMapper`.

---

## Batasan Explicit

**Di luar cakupan**

- Tidak ada versioning dokumentasi (`springdoc.group-configs`). Satu API, satu dokumen.
- Tidak ada Swagger UI sebagai cadangan.
- Tidak ada endpoint actuator untuk Scalar (`scalar.actuator-enabled` dibiarkan `false`).
- Tidak ada generator client SDK dari spec.

**Keterbatasan yang diketahui**

- `script-src` di `/docs` butuh `'unsafe-inline'` karena template Scalar memanggil
  `Scalar.createApiReference()` lewat script inline. Nonce per-request akan lebih ketat,
  tapi butuh menulis ulang template — tidak sebanding untuk halaman yang tidak menerima input
  pengguna. Kelonggaran ini hanya berlaku di `/docs`, tidak menyentuh `/api/**`.
- Scalar tidak punya pre/post-request script seperti Postman. OAuth2 password flow adalah satu-satunya
  jalur otomatis yang tersedia, dan itu mengharuskan endpoint token menerima form-urlencoded.
- `expires_in` dilaporkan dari konfigurasi TTL, bukan dari klaim `exp` token — keduanya berasal
  dari sumber yang sama, jadi tidak akan menyimpang.

**Konsekuensi keputusan #2 yang perlu disadari**

Dokumentasi aktif di semua profile, termasuk `prod`, dan publik tanpa login. Artinya seluruh
permukaan API — path, bentuk request, aturan peran — terbaca siapa pun yang membuka
`/docs` di server produksi. Ini keputusan yang sudah diambil dan rencana ini mengikutinya.
Kalau suatu saat mau dibatasi, satu baris `scalar.enabled=false` di
`application-prod.properties` sudah cukup, tanpa mengubah kode.
