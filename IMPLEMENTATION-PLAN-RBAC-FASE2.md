# Implementation Plan — RBAC Fase 2: Spring Security

Fase 2 memakai tabel yang dibuat di [IMPLEMENTATION-PLAN-RBAC-FASE1.md](./IMPLEMENTATION-PLAN-RBAC-FASE1.md) untuk autentikasi (login karyawan, register + login customer) dan otorisasi berbasis peran.

Prasyarat: fase 1 selesai, migrasi sudah jalan, `./mvnw test` hijau.

---

## 1. Bentuk akhir yang dituju

```
POST /api/auth/karyawan/login     email + password  -> JWT berisi peran dari tabel karyawan_role
POST /api/auth/customer/register  daftar customer baru
POST /api/auth/customer/login     email + password  -> JWT dengan peran CUSTOMER
GET  /api/customer/me             hanya untuk peran CUSTOMER

/api/karyawan/**, /api/payroll/** dijaga @PreAuthorize sesuai matriks di bagian 6
```

Endpoint OWASP yang sudah ada (`/api/safe/login`, `/api/vuln/**`) **tidak disentuh** — itu materi demonstrasi A07 yang punya tujuan sendiri.

---

## 2. Rancangan principal dan token

### 2.1 Tiga sumber user, satu tipe principal

```
findKaryawan(email)  -> tabel karyawan + karyawan_role  -> peran dari DB
findCustomer(email)  -> tabel customer                  -> peran tetap CUSTOMER
demoUsers.get(nama)  -> registry di memori              -> ADMIN / HR / KARYAWAN (materi OWASP)
```

Ketiganya menghasilkan `AppUser` yang sama bentuknya, jadi seluruh kode di hilir (`JwtAuthFilter`, `@PreAuthorize`, `authentication.principal`) tidak perlu tahu user itu datang dari mana.

Urutan pencarian pada `find(username)`: **karyawan → customer → registry demo**. Kunci registry berupa nama pendek (`admin`, `hr`, `karyawan`) sedangkan kunci DB berupa email, jadi ketiga ruang kunci itu tidak mungkin bertabrakan.

### 2.2 `AppUser` — dari satu peran jadi banyak peran

`AppUser` tetap `record` (tidak dikonversi jadi class — aturan `CLAUDE.md` melarang mengonversi record yang sudah ada), hanya komponennya berubah:

```java
public record AppUser(String username, String password, List<String> roles, Integer idKaryawan, String tipe)
        implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }
}
```

`tipe` bernilai `KARYAWAN` atau `CUSTOMER`. Dipakai di response login supaya klien tahu ia masuk sebagai apa; otorisasi sendiri tetap murni bersandar pada `roles`.

Pemanggil `user.role()` yang harus ikut diubah hanya tiga: `JwtService`, `A07SafeController`, `A07VulnController`. Dua controller A07 mengisi `LoginResponse(token, role)` — record itu dibiarkan apa adanya, isinya jadi `String.join(",", user.roles())`. Tidak ada test yang memeriksa field ini, sudah dicek.

### 2.3 Klaim JWT

```java
.claim("roles", user.roles())
.claim("idKaryawan", user.idKaryawan())
.claim("tipe", user.tipe())
```

**`JwtAuthFilter` tidak berubah.** Filter itu sudah memuat ulang user lewat `loadUserByUsername(claims.getSubject())` pada setiap request, jadi otoritas yang dipakai Spring Security selalu yang terbaru dari database — klaim `roles` di dalam token sifatnya informatif untuk klien saja.

Konsekuensinya penting dan layak dijelaskan ke peserta: mencabut peran seseorang langsung berlaku pada request berikutnya, tanpa menunggu token kedaluwarsa. Harganya satu query tambahan per request.

---

## 3. Perubahan file

### File baru

| File | Isi |
|---|---|
| `controller/AuthController.java` | 3 endpoint: login karyawan, register customer, login customer |
| `controller/CustomerController.java` | `GET /api/customer/me` |
| `service/CustomerService.java` | Register + baca profil. **Class biasa tanpa interface** |
| `dto/CustomerRegisterRequest.java` | `nama`, `email`, `password` + Bean Validation |
| `dto/CustomerResponse.java` | `id`, `nama`, `email` — **tanpa password** |
| `dto/AuthResponse.java` | `token`, `tipe`, `roles` |

Semua DTO baru ditulis sebagai class Lombok, bukan record. Request DTO memakai `@Getter @Setter @NoArgsConstructor` supaya deserialisasi Jackson pasti jalan tanpa bergantung pada modul nama parameter; response DTO cukup `@Getter @AllArgsConstructor`.

`CustomerService` sengaja tidak punya interface: implementasinya cuma satu dan tidak akan di-mock ganda (aturan wajib #2 `CLAUDE.md`). Ini menyimpang dari pola `service/` + `service/impl/` yang ada, dan itu keputusan sadar — bukan kelalaian.

### File yang diubah

| File | Perubahan |
|---|---|
| `security/AppUser.java` | `String role` → `List<String> roles`, tambah `String tipe` |
| `security/AppUserDetailsService.java` | Ditulis ulang: cari ke DB dulu, registry demo jadi cadangan |
| `security/JwtService.java` | Klaim `role` → `roles`, tambah klaim `tipe` |
| `owasp/safe/A07SafeController.java` | Menyesuaikan `user.role()` → gabungan `user.roles()` |
| `owasp/vuln/A07VulnController.java` | Sama |
| `config/SecurityConfig.java` | `/api/auth/**` jadi `permitAll` |
| `controller/KaryawanController.java` | `@PreAuthorize` sesuai matriks |
| `controller/PayrollController.java` | `@PreAuthorize` sesuai matriks |

`security/JwtAuthFilter.java` dan `security/LoginAttemptService.java` tidak berubah.

---

## 4. `AppUserDetailsService` — bentuk barunya

```java
public Optional<AppUser> findKaryawan(String email) {
    return karyawanRepository.findByEmailAndDeletedDateIsNull(email)
            .filter(karyawan -> karyawan.getPassword() != null)
            .map(this::toAppUser);
}

public Optional<AppUser> findCustomer(String email) {
    return customerRepository.findByEmailAndDeletedDateIsNull(email)
            .map(customer -> new AppUser(
                    customer.getEmail(), customer.getPassword(), List.of(ROLE_CUSTOMER), null, TIPE_CUSTOMER));
}

public Optional<AppUser> find(String username) {
    return findKaryawan(username)
            .or(() -> findCustomer(username))
            .or(() -> Optional.ofNullable(demoUsers.get(username)));
}

private AppUser toAppUser(Karyawan karyawan) {
    List<String> roles = karyawanRoleRepository.findAllByKaryawan_Id(karyawan.getId()).stream()
            .map(karyawanRole -> karyawanRole.getRole().getNama())
            .toList();
    return new AppUser(karyawan.getEmail(), karyawan.getPassword(), roles, karyawan.getId(), TIPE_KARYAWAN);
}
```

`filter(getPassword() != null)` bukan hiasan. 994 baris karyawan hasil seed punya email tapi `password` NULL, dan `DelegatingPasswordEncoder.matches(raw, null)` **melempar `IllegalArgumentException`**, bukan mengembalikan `false` — tanpa filter itu login dengan email tersebut dibalas 500, bukan 401.

Karyawan tanpa peran tetap lolos filter ini: `roles` yang kosong berarti login berhasil dan setiap endpoint ber-`@PreAuthorize` menolaknya dengan 403. Itu memang perilaku yang ingin didemokan lewat akun `tanparole@masesas.test`.

---

## 5. Endpoint autentikasi

Tiga endpoint berbagi satu helper privat di `AuthController`, sehingga penguncian brute force `LoginAttemptService` (A07) otomatis berlaku juga di jalur login baru:

```java
private ResponseEntity<AuthResponse> login(Optional<AppUser> found, LoginRequest request) {
    if (loginAttempts.isLocked(request.username())) {
        return ResponseEntity.status(HttpStatus.LOCKED).build();
    }
    if (found.isEmpty() || !passwordEncoder.matches(request.password(), found.get().getPassword())) {
        loginAttempts.recordFailure(request.username());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    loginAttempts.reset(request.username());
    AppUser user = found.get();
    return ResponseEntity.ok(new AuthResponse(
            jwtService.issue(user, Instant.now(clock)), user.tipe(), user.roles()));
}
```

`LoginRequest` yang sudah ada dipakai ulang; kolom `username`-nya diisi **email**. Tidak ada record baru dibuat untuk itu.

Register customer menolak email yang sudah terpakai di tabel `customer` **maupun** `karyawan`, dengan melempar `DuplicateResourceException` yang sudah ditangani `GlobalExceptionHandler`. Penjagaan ganda inilah yang menutup satu-satunya jalan munculnya email kembar lintas tabel — sekaligus alasan `find()` boleh memakai urutan pencarian sederhana tanpa perlu memeriksa klaim `tipe`.

---

## 6. Matriks otorisasi

Peran demo lama (`HR`, `KARYAWAN`) sengaja tetap diberi akses yang sama persis seperti sekarang, supaya 90 test OWASP tidak ikut berubah.

| Endpoint | Peran yang diizinkan |
|---|---|
| `POST /api/auth/**` | publik |
| `GET /api/karyawan/**` | ADMIN, MANAGER, MARKETING, SALES, HR, KARYAWAN |
| `PUT /api/karyawan/**` | ADMIN, MANAGER |
| `DELETE /api/karyawan/**` | ADMIN *(sudah berlaku lewat `SecurityConfig`)* |
| `POST`, `GET`, `PUT` `/api/payroll/**` | ADMIN, MANAGER, HR *(satu `@PreAuthorize` di tingkat class)* |
| `DELETE /api/payroll/**` | ADMIN, HR |
| `GET /api/customer/me` | CUSTOMER |
| `/api/karyawan2/**`, `/api/sp/**` | cukup terautentikasi — sengaja dibiarkan, agar perubahan tetap kecil |

Bentuk penulisannya: satu `@PreAuthorize` di tingkat class untuk hak baca, lalu method yang lebih ketat menimpanya. Aturan Spring Security: anotasi di method mengalahkan anotasi di class. Dengan begitu daftar peran yang panjang cukup ditulis sekali.

Peran `MARKETING` dan `SALES` sengaja tidak masuk `PayrollController` sama sekali — mereka bahkan tidak boleh membaca slip gaji. `DELETE` slip gaji dipersempit ke ADMIN dan HR; `HR` dipertahankan karena sebelum perubahan ini memang sudah punya hak tersebut, dan mencabutnya bukan bagian dari permintaan.

Aturan penulisan: `hasAnyRole('ADMIN','MANAGER')`, **tanpa** awalan `ROLE_`. Awalan itu ditambahkan Spring sendiri di `hasRole`/`hasAnyRole`, dan sudah ada di `AppUser.getAuthorities()`. Menulis `hasRole('ROLE_ADMIN')` menghasilkan pencarian `ROLE_ROLE_ADMIN` yang selalu gagal — salah satu bug paling sering muncul saat belajar RBAC.

Sebelum matriks ini dipasang, daftar endpoint yang disentuh test lama diinventarisasi dulu. Kalau ada test yang memanggil endpoint dengan peran di luar daftar, matriksnya yang menyesuaikan — bukan testnya yang dilonggarkan.

---

## 7. Test fase 2

`src/test/java/com/masesas/exercises/demo1/rbac/RbacKaryawanTest.java`:

1. Login `admin@masesas.test` berhasil, response memuat token dan `roles: ["ADMIN"]`
2. Token ADMIN boleh `DELETE /api/karyawan/{id}` → bukan 403
3. Token MARKETING menembak `PUT /api/karyawan/{id}` → 403
4. Token `manager.sales@masesas.test` membawa dua peran, dan lolos di endpoint yang butuh MANAGER
5. Token `tanparole@masesas.test` — login 200, akses `GET /api/karyawan/all` → 403
6. Login email yang password-nya NULL (`karyawan0100@masesas.test`) → **401**, bukan 500
7. Peran berasal dari DB, bukan dari klaim token: terbitkan token untuk `marketing@masesas.test` yang klaim `roles`-nya dipalsukan jadi `["ADMIN"]`, lalu tembak endpoint khusus ADMIN → tetap 403

Cara nomor 7 dipilih karena tidak menyentuh data: memalsukan klaim jauh lebih murah dan lebih tajam daripada menghapus baris `karyawan_role` di database bersama lalu mengembalikannya.

`src/test/java/com/masesas/exercises/demo1/rbac/RbacCustomerTest.java`:

1. Register customer baru → 201, response tidak memuat `password`
2. Register email yang sudah ada → 409
3. Register memakai email milik karyawan → 409
4. Login customer → token dengan `tipe: CUSTOMER`, `roles: ["CUSTOMER"]`
5. Token customer menembak `GET /api/karyawan/all` → 403
6. Token karyawan menembak `GET /api/customer/me` → 403
7. `GET /api/customer/me` dengan token customer → 200 dan datanya milik dirinya sendiri

Dua hal yang harus dibersihkan di `@BeforeEach`/`@AfterEach` supaya test bisa dijalankan berulang kali di database bersama:

- `loginAttempts.reset(...)` untuk email yang sengaja gagal login — tanpa ini hitungan kegagalan mengendap 15 menit di Redis dan menjatuhkan test pada eksekusi berikutnya
- Baris customer hasil register dihapus lagi lewat `CustomerRepository`

`rateLimitFilter.bersihkan()` **tidak** diperlukan: `RateLimitFilter` hanya memfilter `/api/safe/login` dan `/api/safe/karyawan/search`, sedangkan endpoint baru berada di `/api/auth/**`. Pembatasan laju di jalur baru dijaga oleh penguncian akun `LoginAttemptService`, bukan oleh bucket per-IP.

---

## 8. Checklist eksekusi

- [x] Ubah `AppUser` jadi `roles` + `tipe`, sesuaikan `JwtService` dan dua controller A07
- [x] `./mvnw test` — 90 test lama masih hijau **sebelum** menambah fitur baru
- [x] Tulis ulang `AppUserDetailsService` (DB dulu, registry cadangan, filter password NULL)
- [x] `./mvnw test` lagi — registry demo terbukti masih berfungsi
- [x] Buat DTO, `CustomerService`, `AuthController`, `CustomerController`
- [x] Buka `/api/auth/**` di `SecurityConfig`
- [x] Verifikasi `DuplicateResourceException` dipetakan ke 409 di `GlobalExceptionHandler`
- [x] Inventarisasi endpoint yang disentuh test lama — seluruhnya memakai akun `hr` dan `karyawan`, jadi matriks cukup mempertahankan kedua peran itu
- [x] Pasang `@PreAuthorize` sesuai matriks
- [x] Tulis `RbacKaryawanTest` dan `RbacCustomerTest`
- [x] `./mvnw test` penuh — **106/106 hijau** (90 lama + 16 baru)
- [x] Perbarui `TOP10-OWASP.md` bagian A01 dengan matriks dan rujukan ke RBAC baru
- [ ] Commit `feat: RBAC karyawan dan customer dengan Spring Security`

---

## 9. Batasan explicit

**Di luar ruang lingkup:**
- Refresh token, logout, dan pencabutan token (token STATELESS, TTL 15 menit)
- Endpoint mengelola peran lewat API — peran diatur lewat SQL
- Ganti password, lupa password, verifikasi email
- `RoleHierarchy` — daftar peran ini datar
- Data milik customer (order, transaksi). Customer baru bisa register, login, dan membaca profilnya sendiri

**Keterbatasan yang diterima:**
- `@Valid` pada `@RequestBody` berjalan **sebelum** `@PreAuthorize`. Request dengan body tidak valid dari user yang tidak berhak dibalas 400, bukan 403 — urutannya begitu karena argumen handler di-resolve sebelum proxy keamanan dipanggil. Tidak berbahaya di sini (pesan 400 hanya menyebut field yang salah), tapi perlu diketahui saat membaca kode
- Dua query tambahan per request (karyawan + peran) untuk memuat ulang principal. Bisa di-cache Redis kalau nanti terasa, tapi menambahkan cache sekarang berarti menambah jalur invalidasi yang tidak dibutuhkan siapa pun
- Registry akun demo di memori hidup berdampingan dengan user DB. Dipertahankan semata demi materi OWASP; di aplikasi nyata registry seperti ini tidak punya tempat
- Peran `HR` dan `KARYAWAN` hanya ada di registry, tidak ada di tabel `role` — keduanya milik materi A01/A07, bukan bagian dari RBAC baru
- `roles` di dalam JWT bisa basi bila peran berubah setelah token terbit. Tidak berbahaya: yang menentukan izin adalah hasil pembacaan DB di `JwtAuthFilter`, bukan isi klaim
