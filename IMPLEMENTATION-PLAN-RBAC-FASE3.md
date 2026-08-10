# Implementation Plan — RBAC Fase 3: Role Sepenuhnya dari Database

Menghapus `demoUsers` yang di-hardcode di `AppUserDetailsService`, sehingga seluruh
peran karyawan bersumber tunggal dari tabel `role` dan `karyawan_role`. Sekaligus
memperkenalkan peran `GUEST` untuk pengunjung anonim.

Lanjutan dari [IMPLEMENTATION-PLAN-RBAC-FASE1.md](./IMPLEMENTATION-PLAN-RBAC-FASE1.md)
dan [IMPLEMENTATION-PLAN-RBAC-FASE2.md](./IMPLEMENTATION-PLAN-RBAC-FASE2.md).

---

## 1. Latar Belakang — Hasil Penelusuran Kode

Fase 2 memindahkan otorisasi ke database tetapi tidak mencabut jalur lama. Akibatnya
sekarang ada **dua kosakata peran yang tidak pernah bertemu**.

`AppUserDetailsService.java:41-45` membangun tiga akun in-memory saat konstruksi:

```java
registry.put("admin",    new AppUser("admin",    encoded, List.of("ADMIN"),    null, TIPE_KARYAWAN));
registry.put("hr",       new AppUser("hr",       encoded, List.of("HR"),       null, TIPE_KARYAWAN));
registry.put("karyawan", new AppUser("karyawan", encoded, List.of("KARYAWAN"), 1,    TIPE_KARYAWAN));
```

`find()` pada baris 55-58 menjadikannya fallback terakhir setelah pencarian database.

| Peran | Tabel `role` | `demoUsers` | Dipakai `@PreAuthorize` |
|---|---|---|---|
| ADMIN | ada | ada | ada |
| MANAGER | ada | — | ada |
| MARKETING | ada | — | ada |
| SALES | ada | — | ada |
| **HR** | **tidak ada** | ada | ada (4 tempat) |
| **KARYAWAN** | **tidak ada** | ada | ada (1 tempat) |
| CUSTOMER | tidak ada | disintesis `findCustomer()` | ada (1 tempat) |

### Konsekuensi yang terukur

1. **`hasRole('HR')` tidak pernah cocok dengan pengguna database.** Endpoint
   hapus slip gaji `PayrollController.java:108` (`hasAnyRole('ADMIN','HR')`)
   efektif ADMIN-only di produksi. Sama untuk `PayrollController.java:40`
   (`hasAnyRole('ADMIN','MANAGER','HR')`, tingkat kelas), `A01SafeController.java:27`,
   dan `A02SafeController.java:27`. Arahnya fail-closed, jadi ini bukan kerentanan —
   tetapi aturan aksesnya fiktif.

2. **Test OWASP dan test RBAC menguji dua jalur berbeda.** Sebelas berkas test
   OWASP login sebagai `"hr"`/`"karyawan"` (akun in-memory); `RbacKaryawanTest`
   login sebagai `admin@masesas.test` (database). Test A01 hijau, tetapi yang
   dibuktikannya adalah otorisasi lewat akun hardcoded — bukan lewat `karyawan_role`.

3. **Kredensial bersama di bean produksi.** `app.security.password` (dari
   `DEMO_PASSWORD` di `.env`) dipakai `AppUserDetailsService` untuk meng-encode tiga
   akun tersebut, dan dibocorkan lewat `rawPassword()` yang dipakai
   `A07VulnController.java:34` untuk membandingkan password dalam bentuk plaintext.

---

## 2. Keputusan Rancangan

### K1 — `HR` dan `KARYAWAN` menjadi baris di tabel `role`

Bukan menghapus anotasi yang memakainya. Keduanya peran yang sah dan sudah dirujuk
lima tempat di kode; yang salah adalah tempat penyimpanannya.

### K2 — `demoUsers` dan `rawPassword()` dihapus seluruhnya

Setelah K1, tidak ada lagi yang hanya bisa dijangkau lewat akun in-memory.
`find()` menjadi dua cabang: karyawan lalu customer.

### K3 — `CUSTOMER` tetap disintesis di `findCustomer()`, tidak masuk tabel `role`

Customer bukan karyawan dan tidak punya baris `karyawan_role`. Peran itu melekat
pada tipe principal, bukan pada penugasan yang bisa dicabut. Memasukkannya ke tabel
`role` akan menghasilkan baris yang tidak pernah direferensikan `karyawan_role`.

### K4 — `GUEST` adalah authority principal anonim, bukan baris tabel

Sesuai keputusan: guest adalah pengunjung tanpa akun. Karena tidak ada akun, tidak
ada yang bisa ditugasi peran ini — baris di tabel `role` justru akan menyesatkan.

Implementasinya memakai `AnonymousConfigurer` bawaan Spring Security:

```java
.anonymous(anonymous -> anonymous.principal("guest").authorities("ROLE_GUEST"))
```

API terverifikasi ada di spring-security-config **7.0.2** (Spring Boot 4.1.0) —
`principal(Object)` dan `authorities(String...)` keduanya publik.

### K5 — Setiap principal yang login juga membawa `ROLE_GUEST`

`AppUser.getAuthorities()` menambahkan `ROLE_GUEST` di samping peran databasenya.
Dengan begitu `hasRole('GUEST')` berarti **"siapa pun, login atau tidak"** — bukan
"khusus yang belum login".

Tanpa ini, `@PreAuthorize("hasRole('GUEST')")` pada endpoint product nanti akan
menolak karyawan yang sudah login — jebakan yang baru ketahuan saat product dibuat.
Semantiknya ditetapkan sekarang dan dikunci dengan test.

### K6 — `A07VulnController` beralih ke `passwordEncoder.matches()`

Kerentanan yang didemokan A07 adalah **tidak adanya penguncian akun** dan **token
tanpa masa berlaku** — bukan perbandingan plaintext. Mengganti pembandingnya
menghapus `rawPassword()` tanpa mengurangi satu pun kerentanan yang diuji.

### Alternatif yang ditolak

| Alternatif | Alasan ditolak |
|---|---|
| Memindahkan `demoUsers` ke `@Profile("test")` | Dua kosakata peran tetap ada, hanya berpindah tempat. Test tetap tidak menguji `karyawan_role`. |
| Menambah `GUEST` ke tabel `role` | Tidak ada akun yang bisa ditugasi. Baris yatim. |
| Menghapus `hasRole('HR')` dan menggantinya `MANAGER` | Mengubah aturan bisnis (siapa boleh approve payroll) di tengah perubahan teknis. Dua perubahan bercampur. |
| Tabel `permission` + `role_permission` | Di luar cakupan; tidak menambal lubang apa pun yang ada. Lihat pembahasan sebelumnya. |

---

## 3. Perubahan per Berkas

### 3.1 Migrasi database — `rbac_role_penuh_masesas.sql` (baru)

Mengikuti pola Fase 1: hash password dipasok lewat variabel psql, bukan ditulis di berkas.

```sql
BEGIN;

-- 1. Dua peran yang selama ini hanya hidup di kode
INSERT INTO masesas.role (nama)
VALUES ('HR'), ('KARYAWAN')
ON CONFLICT (nama) DO NOTHING;

-- 2. Akun pengganti demo user in-memory.
--    Karyawan 6 sengaja TIDAK disentuh: RbacKaryawanTest bergantung pada
--    keberadaan karyawan yang login berhasil tapi tanpa peran sama sekali.
UPDATE masesas.karyawan SET email = 'hr@masesas.test',       password = :'pwd_hash' WHERE id = 7;
UPDATE masesas.karyawan SET email = 'karyawan@masesas.test', password = :'pwd_hash' WHERE id = 8;

-- 3. Pemberian peran
INSERT INTO masesas.karyawan_role (id_karyawan, id_role)
SELECT v.id_karyawan, r.id
  FROM (VALUES (7, 'HR'),
               (8, 'KARYAWAN')) AS v(id_karyawan, nama)
  JOIN masesas.role r     ON r.nama = v.nama
  JOIN masesas.karyawan k ON k.id = v.id_karyawan
ON CONFLICT (id_karyawan, id_role) DO NOTHING;

COMMIT;
```

Idempoten: `ON CONFLICT DO NOTHING` pada kedua INSERT, `UPDATE` menulis nilai yang
sama bila diulang.

### 3.2 `security/AppUserDetailsService.java`

Hapus: field `demoUsers`, field `rawPassword`, method `rawPassword()`, dependensi
`PasswordEncoder`, dan `@Value("${app.security.password}")`. Konstruktor eksplisit
diganti `@RequiredArgsConstructor` sesuai konvensi proyek.

```java
public Optional<AppUser> find(String username) {
    return findKaryawan(username).or(() -> findCustomer(username));
}
```

`findKaryawan`, `findCustomer`, `toAppUser`, dan `loadUserByUsername` tidak berubah.

### 3.3 `security/AppUser.java`

```java
public static final String ROLE_GUEST = "ROLE_GUEST";

@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return Stream.concat(roles.stream().map(role -> "ROLE_" + role), Stream.of(ROLE_GUEST))
            .map(SimpleGrantedAuthority::new)
            .toList();
}
```

### 3.4 `config/SecurityConfig.java`

Satu baris di rantai `HttpSecurity`, sebelum `.exceptionHandling(...)`:

```java
.anonymous(anonymous -> anonymous.principal("guest").authorities("ROLE_GUEST"))
```

`anyRequest().authenticated()` tetap. Principal anonim tidak lolos `authenticated()`,
jadi deny-by-default tidak berubah — endpoint publik nanti tetap harus didaftarkan
eksplisit.

### 3.5 `owasp/vuln/A07VulnController.java`

```java
if (found.isEmpty() || !passwordEncoder.matches(request.getPassword(), found.get().getPassword())) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
}
```

Tambah field `private final PasswordEncoder passwordEncoder;`. Tetap tanpa
`LoginAttemptService` dan tetap memakai `issueWithoutExpiry` — dua kerentanan A07 utuh.

### 3.6 `src/main/resources/application.properties`

`app.security.password=${DEMO_PASSWORD}` tidak lagi dibaca kode produksi mana pun,
tetapi tetap dipertahankan sebagai sumber password untuk test (lihat 3.7). Komentarnya
diperbarui supaya tidak lagi menyebut akun demo in-memory.

`DEMO_PASSWORD` di `.env` tetap dibutuhkan — dipakai `rbac_masesas.sql` dan
`rbac_role_penuh_masesas.sql` saat menyiapkan hash.

### 3.7 Test — 11 berkas

Pemetaan yang berlaku di semua berkas:

| Sebelum | Sesudah |
|---|---|
| `"hr"` | `"hr@masesas.test"` |
| `"karyawan"` | `"karyawan@masesas.test"` |
| `"admin"` | `"admin@masesas.test"` |

| Berkas | Titik sentuh | Catatan khusus |
|---|---|---|
| `owasp/A01AccessControlTest.java` | 5 | `ID_KARYAWAN_SENDIRI` 1 → **8** (id `karyawan@masesas.test`); `ID_KARYAWAN_ORANG_LAIN` tetap 2 |
| `owasp/A09LoggingTest.java` | 5 | `aktor=hr` → `aktor=hr@masesas.test`; `loginRequest()` mengirim email |
| `config/SecurityWhitelistTest.java` | 5 | `loginAttempts.reset()` memakai email |
| `owasp/A04InsecureDesignTest.java` | 4 | idem `reset()` |
| `owasp/A02CryptoTest.java` | 1 | — |
| `owasp/A03SqlInjectionTest.java` | 1 | — |
| `owasp/A03XssTest.java` | 1 | — |
| `owasp/A05MisconfigTest.java` | 1 | — |
| `owasp/A07AuthTest.java` | 1 | — |
| `owasp/A08IntegrityTest.java` | 1 | — |
| `owasp/A10SsrfTest.java` | 1 | — |

`service/KaryawanServiceCacheTest.java` **tidak** ikut berubah — string `"karyawan"`
di sana adalah nama cache, bukan username.

### 3.8 Test — password demo tidak lagi ditulis di source

Literal `"Password123!"` muncul di lima berkas, semuanya sudah masuk daftar 3.7:

| Berkas | Baris |
|---|---|
| `owasp/A04InsecureDesignTest.java` | 150 |
| `config/SecurityWhitelistTest.java` | 83 |
| `owasp/A05MisconfigTest.java` | 36 |
| `owasp/A07AuthTest.java` | 36 |
| `owasp/A09LoggingTest.java` | 141 |

Empat berkas pertama mengambil nilai dari `@Value("${app.security.password}")` —
properti yang sama dengan yang dipakai menyiapkan hash saat migrasi, jadi test dan
data seed dijamin sinkron.

`A05MisconfigTest` **dikecualikan**: literalnya di sana bukan kredensial untuk login,
melainkan anggota array `KREDENSIAL_YANG_TIDAK_BOLEH_ADA` — daftar string yang
dipastikan tidak pernah bocor ke response. Nilainya justru harus diketahui test untuk
bisa dicari, dan array itu `static final`.

---

## 4. Urutan Eksekusi

Migrasi SQL wajib jalan **sebelum** kompilasi test, karena test menembak PostgreSQL
sungguhan (tidak ada Testcontainers di proyek ini).

1. Jalankan `rbac_role_penuh_masesas.sql` ke `binar_finance`, verifikasi lewat query
   pemeriksaan di akhir berkas.
2. `AppUser.getAuthorities()` + `SecurityConfig` (perubahan aditif, belum memutus apa pun).
3. `A07VulnController` beralih ke `passwordEncoder`.
4. `AppUserDetailsService` — cabut `demoUsers` dan `rawPassword()`. **Kompilasi patah di sini** sampai langkah 5 selesai.
5. Sebelas berkas test disesuaikan.
6. Test baru (bagian 5).
7. `./mvnw test` hijau.
8. `TOP10-OWASP.md` diperbarui: bagian A01 menyebut peran bersumber dari database.

---

## 5. Test

### Test baru — `rbac/RbacRoleDatabaseTest.java`

| # | Nama | Yang dibuktikan |
|---|---|---|
| 1 | `demoUserLamaTidakDikenalLagi` | `loadUserByUsername("hr")` melempar `UsernameNotFoundException` |
| 2 | `peranDimuatDariKaryawanRole` | `findKaryawan("hr@masesas.test")` menghasilkan `roles` berisi `HR` |
| 3 | `peranHrDariDatabaseLolosOtorisasiPayroll` | DELETE slip gaji dengan token HR database → 404 (lolos otorisasi, gagal di data); dengan MARKETING → 403 |
| 4 | `pencabutanPeranLangsungBerlaku` | hapus baris `karyawan_role`, pemuatan berikutnya tidak lagi membawa peran itu |

### Test baru — `rbac/RbacGuestTest.java`

| # | Nama | Yang dibuktikan |
|---|---|---|
| 5 | `anonimMembawaRoleGuest` | request tanpa token ke `/api/safe/login` → baris audit berisi `aktor=guest` dan `peran=ROLE_GUEST`. Diamati lewat `AuditLogger` karena `SecurityContext` sudah dibersihkan saat MockMvc mengembalikan hasil |
| 6 | `anonimTetapDitolakDiEndpointTerlindung` | GET `/api/karyawan/all` tanpa token → 401 |
| 7 | `penggunaLoginJugaMembawaRoleGuest` | authority karyawan HR = `ROLE_HR` **dan** `ROLE_GUEST` |

### Test yang wajib tetap hijau tanpa perubahan makna

`RbacKaryawanTest` (6 kasus, termasuk karyawan tanpa peran → 403) dan
`RbacCustomerTest` — keduanya sudah memakai jalur database sejak Fase 2.

---

## 6. Verifikasi Silang

Diperiksa satu per satu terhadap kode saat ini:

- **`@PreAuthorize` yang ada** — enam peran yang dirujuk (`ADMIN`, `MANAGER`,
  `MARKETING`, `SALES`, `HR`, `KARYAWAN`) semuanya ada di tabel `role` setelah
  migrasi. `CUSTOMER` tetap dari `findCustomer()`. Tidak ada anotasi yang perlu diubah.
- **`A09LoggingTest` assertion `peran=ROLE_HR`** — setelah K5 nilainya menjadi
  `ROLE_HR,ROLE_GUEST`. Assertion memakai `String.contains`, jadi tetap lolos.
  Rapuh terhadap urutan, karena itu `ROLE_GUEST` sengaja ditempatkan **setelah**
  peran database, bukan sebelum.
- **`AuditLogger.aktor()`** — untuk lalu lintas anonim nilainya berubah dari
  `anonymousUser` menjadi `guest`. Tidak ada assertion yang menyentuhnya
  (diperiksa: nol kecocokan untuk `anonymousUser` di seluruh `src`). Konstanta
  `ANONIM` sudah mati sejak dulu (anonymous filter aktif secara default membuat
  `authentication` tidak pernah `null`) dan dibiarkan apa adanya.
- **`JwtAuthFilter`** — dipasang sebelum `UsernamePasswordAuthenticationFilter`;
  `AnonymousAuthenticationFilter` berjalan setelahnya dan hanya mengisi konteks yang
  masih kosong. Token yang sah tidak akan tertimpa principal anonim.
- **`LoginAttemptService`** — kunci penguncian adalah string username. Setelah
  migrasi, kuncinya menjadi email. Semua `reset()` di test ikut berubah, jika tidak
  akun akan tetap terkunci antar-test (pernah terjadi di Fase A05/A08).
- **`CustomerService.register()`** — menolak email yang sudah dipakai karyawan.
  `hr@masesas.test` dan `karyawan@masesas.test` ikut terlindungi otomatis.
- **Password demo di source** — `"Password123!"` ditulis di lima berkas test
  (bagian 3.8), semuanya sudah tercakup daftar berkas yang disentuh.

---

## 7. Batasan Explicit

**Di luar cakupan**

- Tabel `produk` dan endpoint yang menampilkannya. `ROLE_GUEST` disiapkan dan diuji,
  tetapi belum ada endpoint yang memakainya — akan menyusul di rencana terpisah.
- Tabel `permission` / `role_permission`. Otorisasi tetap berbasis peran.
- Manajemen peran lewat API (belum ada endpoint tambah/cabut peran; masih lewat SQL).

**Known limitations**

- **Peran dibaca ulang setiap request.** `JwtAuthFilter` memanggil
  `loadUserByUsername`, yang berarti satu query `karyawan_role` per request. Ini
  disengaja: pencabutan peran langsung berlaku tanpa menunggu token kedaluwarsa
  (diuji kasus #4). Biayanya belum di-cache.
- **Klaim `roles` di dalam JWT tidak dipakai untuk otorisasi.** `JwtService` menuliskannya,
  tetapi authority diambil dari database saat filter berjalan. Klaim itu informatif untuk klien.
- **Test bergantung pada PostgreSQL bersama.** Migrasi harus dijalankan lebih dulu
  di setiap lingkungan tempat test dijalankan.
- **`hasRole('GUEST')` tidak pernah menolak siapa pun** setelah K5. Nilainya adalah
  menyatakan niat "endpoint ini publik" secara eksplisit, bukan menyaring.

**Known edge cases**

- Karyawan id 6 (`tanparole@masesas.test`) tetap tanpa peran database. Setelah K5 ia
  memegang `ROLE_GUEST` saja — login berhasil, hampir semua endpoint 403. Ini perilaku
  yang dikehendaki dan sudah diuji `RbacKaryawanTest`.
- Bila migrasi 3.1 dijalankan tetapi `karyawan` id 7/8 tidak ada di database,
  `UPDATE` tidak mengenai baris apa pun dan `INSERT ... JOIN karyawan` tidak
  menghasilkan baris. Test akan gagal dengan 401, bukan error yang menyesatkan.
  Query pemeriksaan di akhir berkas migrasi menampilkan hal ini sebelum test dijalankan.

---

## 8. Definition of Done

- [ ] `rbac_role_penuh_masesas.sql` dijalankan; query pemeriksaan menampilkan
      `hr@masesas.test` → HR dan `karyawan@masesas.test` → KARYAWAN
- [ ] `grep -rn "demoUsers\|rawPassword" src/main` → nol kecocokan
- [ ] Tidak ada peran yang dirujuk `@PreAuthorize` di luar tabel `role`, kecuali
      `CUSTOMER` (K3) dan `GUEST` (K4)
- [ ] `./mvnw clean compile` bersih
- [ ] `./mvnw test` hijau — 108 test lama + 7 test baru = 115
- [ ] Tidak ada berkas `.java` baru yang berisi komentar
- [ ] `"Password123!"` tidak lagi muncul sebagai kredensial login di `src/test`
      (satu-satunya sisa: array deteksi kebocoran di `A05MisconfigTest`)
- [ ] `TOP10-OWASP.md` bagian A01 diperbarui
