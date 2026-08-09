# OWASP Top 10:2021 — Penjelasan & Peta Lokasi

Dokumen ini menjawab dua hal:

1. **Apa itu** tiap kategori OWASP, dijelaskan dengan bahasa sederhana.
2. **Di mana** implementasinya berada di proyek ini.

Setiap kategori punya **dua versi**: versi rentan (`/api/vuln/...`) dan versi aman (`/api/safe/...`). Jalankan serangan yang sama ke keduanya — berhasil di yang rentan, gagal di yang aman.

> Cara membangunnya ada di [IMPLEMENTATION-PLAN.md](./IMPLEMENTATION-PLAN.md).

---

## ⚠️ Peringatan

Package `owasp/vuln` berisi **kerentanan sungguhan**, bukan simulasi. Dipagari `@Profile("owasp-demo")` sehingga tidak aktif kecuali dinyalakan sengaja.

**Jangan pernah men-deploy aplikasi ini dengan profil `owasp-demo` menyala.**

```bash
# menyalakan endpoint rentan (hanya di komputer sendiri)
mvn spring-boot:run -Dspring-boot.run.profiles=owasp-demo
```

---

## Indeks

| # | Kategori | Endpoint rentan | Endpoint aman | Status |
|---|---|---|---|---|
| [A01](#a01--broken-access-control) | Broken Access Control | `/api/vuln/payroll/{id}` | `/api/safe/payroll/{id}` | ✅ **Selesai** |
| [A02](#a02--cryptographic-failures) | Cryptographic Failures | `/api/vuln/karyawan/{id}/detail` | `/api/safe/karyawan/{id}/detail` | ✅ **Selesai** |
| [A03-SQL](#a03--injection--sql) | Injection — SQL | `/api/vuln/karyawan/search` | `/api/safe/karyawan/search` | ✅ **Selesai** |
| [A03-XSS](#a03--injection--xss) | Injection — XSS | `POST /api/vuln/karyawan/teks` | `POST /api/safe/karyawan/teks` | ✅ **Selesai** |
| [A04](#a04--insecure-design) | Insecure Design | `/api/vuln/login`, `PUT /api/vuln/payroll/…` | `/api/safe/login`, `PUT /api/payroll/…` | ✅ **Selesai** |
| [A05](#a05--security-misconfiguration) | Security Misconfiguration | — *(konfigurasi)* | — *(konfigurasi)* | ✅ **Selesai** |
| [A06](#a06--vulnerable-and-outdated-components) | Vulnerable & Outdated Components | — *(pom.xml)* | — *(pom.xml)* | ⬜ Belum |
| [A07](#a07--identification-and-authentication-failures) | Identification & Authentication Failures | `POST /api/vuln/login` | `POST /api/safe/login` | ✅ **Selesai** |
| [A08](#a08--software-and-data-integrity-failures) | Software & Data Integrity Failures | `PUT /api/vuln/payroll/…/tanpa-versi` | `PUT /api/payroll/…` | ✅ **Selesai** |
| [A09](#a09--security-logging-and-monitoring-failures) | Security Logging & Monitoring Failures | — *(aspect)* | — *(aspect)* | ✅ **Selesai** |
| [A10](#a10--server-side-request-forgery-ssrf) | Server-Side Request Forgery | `POST /api/vuln/karyawan/{id}/foto` | `POST /api/safe/karyawan/{id}/foto` | ⬜ Belum |

Tiga kategori (A05, A06, A09) tidak berbentuk endpoint — bedanya ada di file konfigurasi, bukan di URL.

---

## A01 — Broken Access Control

**Sederhananya:** aplikasi tahu siapa kamu, tapi lupa mengecek apakah kamu **boleh** melakukan sesuatu.

Ibarat kantor yang memeriksa kartu pegawai di pintu depan, lalu membiarkan semua orang masuk ke ruang keuangan.

Dua bentuk yang dibuat di sini:

- **Tanpa cek peran** — karyawan biasa bisa menghapus data karyawan lain.
- **IDOR** *(Insecure Direct Object Reference)* — cukup ganti angka di URL, `/payroll/1` jadi `/payroll/2`, dan slip gaji orang lain terbuka.

| | Lokasi |
|---|---|
| Rentan | `owasp/vuln/A01VulnController.java` |
| Aman | `owasp/safe/A01SafeController.java` |
| Pendukung | `config/SecurityConfig.java`, `controller/PayrollController.java`, `exception/GlobalExceptionHandler.java` |
| Test | `owasp/A01AccessControlTest.java` — 7 test, semua lulus |

**Cara amannya:** aturan per-endpoint di `SecurityConfig` ditambah `@PreAuthorize` di tiap method. Untuk IDOR, dicek bahwa ID yang diminta memang milik si pengguna:

```java
@PreAuthorize("hasRole('HR') or #idKaryawan == authentication.principal.idKaryawan")
```

`authentication.principal` di sini adalah `AppUser`, yang membawa `idKaryawan`. Itulah alasan A07 harus selesai lebih dulu — tanpa principal, ekspresi ini tidak punya apa pun untuk dibandingkan.

### Aturan otorisasi yang berlaku sekarang

| Path | Aturan |
|---|---|
| `/api/safe/login` | terbuka |
| `/api/vuln/**` | terbuka — memang begitu maunya |
| `DELETE /api/karyawan/**`, `/api/karyawan2/**` | `ROLE_ADMIN` |
| `POST`/`PUT`/`DELETE` `/api/payroll/**` | `ROLE_HR` |
| sisanya | wajib token |

Tanpa token dibalas **401**; token sah tapi peran kurang dibalas **403**.

### Mencobanya

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/safe/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"karyawan","password":"Password123!"}' | jq -r .token)

# slip gaji sendiri (idKaryawan=1) -> 200
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/api/safe/payroll/1 \
  -H "Authorization: Bearer $TOKEN"

# slip gaji orang lain -> 403
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/api/safe/payroll/2 \
  -H "Authorization: Bearer $TOKEN"

# versi rentan: tanpa token pun terbaca -> 200
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/api/vuln/payroll/2
```

> **Perubahan yang memutus kompatibilitas:** sejak A01, seluruh endpoint lama (`/api/karyawan`, `/api/payroll`, `/api/sp/...`) menuntut header `Authorization: Bearer <token>`. Sebelumnya semuanya terbuka.

---

## A02 — Cryptographic Failures

**Sederhananya:** data sensitif disimpan atau dikirim tanpa perlindungan.

Bukan cuma soal "tidak dienkripsi" — memakai algoritma usang seperti MD5 juga masuk kategori ini.

Data sensitif di proyek ini: **NIK**, **NPWP**, dan **nominal gaji**.

Dua bentuk yang dibuat di sini:

- **Tersimpan polos** — NIK dan NPWP tersimpan apa adanya di database, siapa pun yang bisa membaca tabel langsung melihatnya.
- **Hash lemah** — password di-hash dengan MD5 yang bisa dibongkar dalam hitungan detik.

| | Lokasi |
|---|---|
| Rentan | `owasp/vuln/A02VulnController.java` |
| Aman | `owasp/safe/A02SafeController.java` |
| Pendukung | `owasp/safe/CryptoConverter.java`, `owasp/safe/EnkripsiDetailRunner.java`, `entity/DetailKaryawan.java`, `dto/DetailKaryawanResponse.java`, `config/SecurityConfig.java` |
| Migrasi | `detail_karyawan_enkripsi_masesas.sql` |
| Test | `owasp/A02CryptoTest.java` — 8 test, semua lulus |

**Cara amannya:** NIK dan NPWP dienkripsi AES-GCM otomatis saat masuk database (lewat `AttributeConverter`), password pakai bcrypt, dan response hanya menampilkan versi tersamar `************0001`.

### Enkripsi at-rest lewat AttributeConverter

`@Convert(converter = CryptoConverter.class)` pada field `nik` dan `npwp` membuat enkripsi terjadi **otomatis di lapisan JPA**. Tidak ada satu pun kode service atau controller yang perlu tahu soal enkripsi — mereka membaca dan menulis String biasa. Ini penting: kalau enkripsi dipanggil manual, satu jalur yang lupa memanggilnya langsung membocorkan data.

Format tersimpan: `enc:v1:` + Base64(IV ‖ ciphertext ‖ tag).

| Bagian | Alasan |
|---|---|
| Penanda `enc:v1:` | Membedakan data terenkripsi dari teks biasa. Baris lama tetap terbaca selama masa transisi, dan versinya bisa dinaikkan kalau algoritma berganti |
| IV 12 byte acak per nilai | Dua orang ber-NIK sama menghasilkan ciphertext berbeda. Tanpa ini, pola berulang bisa dibaca tanpa perlu memecahkan kunci |
| GCM tag 128 bit | Mendeteksi kalau ciphertext diubah orang. AES tanpa mode berautentikasi tidak tahu bedanya |

### Masking: aman secara bawaan

`DetailKaryawanResponse.from()` **selalu** menyamarkan. Untuk menampilkan nomor utuh harus sengaja memanggil `fromLengkap()`, yang hanya dipakai controller demo. Arah defaultnya dibalik: lupa berarti aman, bukan lupa berarti bocor.

### Password

`DelegatingPasswordEncoder` dengan bcrypt strength 12 menggantikan `BCryptPasswordEncoder` polos. Hash-nya sekarang berawalan algoritma: `{bcrypt}$2a$12$...`. Tanpa awalan itu, mengganti algoritma di kemudian hari berarti seluruh password lama tidak bisa lagi diverifikasi.

Bandingkan dengan MD5 di endpoint rentan — `password123` **selalu** menghasilkan `482c811da5d5b4bc6d497ffa98491e38`, nilai yang bisa dicari di tabel pelangi mana pun dalam hitungan detik.

### Migrasi

Dua langkah, keduanya aman diulang:

```bash
# 1. perlebar kolom - varchar(20) tidak muat ciphertext
psql ... -f detail_karyawan_enkripsi_masesas.sql

# 2. enkripsi baris yang masih teks biasa
mvn spring-boot:run -Dspring-boot.run.profiles=migrasi-enkripsi
```

> ⚠️ **Kunci hilang berarti data hilang.** `app.crypto.key` adalah satu-satunya cara membaca kembali NIK dan NPWP yang sudah dienkripsi. Kunci ini pindah ke `.env` pada [A05](#a05--security-misconfiguration) dengan **nilai yang sama persis** — mengganti nilainya membuat seluruh data terenkripsi tidak terbaca.

### Mencobanya

```bash
# RENTAN: NIK dan NPWP utuh
curl -s localhost:8080/api/vuln/karyawan/1/detail

# AMAN: hanya 4 digit terakhir
curl -s localhost:8080/api/safe/karyawan/1/detail -H "Authorization: Bearer $TOKEN"

# RENTAN: MD5 selalu sama, bisa dicari di tabel pelangi
curl -s 'localhost:8080/api/vuln/hash?password=password123'
```

---

## A03 — Injection — SQL

**Sederhananya:** input dari user ikut terbaca sebagai **perintah**, bukan sekadar data.

Kalau query dirangkai dengan menyambung teks:

```
SELECT * FROM karyawan WHERE nama = '<input user>'
```

lalu user mengetik `' OR '1'='1`, query berubah artinya jadi "ambil semua baris".

Dua bentuk yang dibuat di sini:

- **Penggabungan string** pada kondisi `WHERE`.
- **Kolom `ORDER BY` dari input user** — ini tidak bisa diamankan dengan bind parameter, harus pakai allowlist.

| | Lokasi |
|---|---|
| Rentan | `owasp/vuln/A03SqlVulnController.java` |
| Aman | `owasp/safe/A03SqlSafeController.java` |
| Pendukung | `owasp/safe/SortField.java`, anotasi validasi di 5 DTO, `exception/GlobalExceptionHandler.java` |
| Test | `owasp/A03SqlInjectionTest.java` — 7 test, semua lulus |

**Cara amannya:** semua nilai lewat bind parameter `?`, nama kolom sort dibatasi daftar tetap (`enum SortField`), dan seluruh input divalidasi lebih dulu dengan `@Valid`.

### Kenapa `ORDER BY` butuh perlakuan berbeda

Bind parameter `?` hanya bisa menggantikan **nilai**, tidak bisa menggantikan **nama kolom**. Jadi `ORDER BY ?` tidak akan pernah bekerja. Satu-satunya cara aman adalah membandingkan input dengan daftar tetap:

```java
SortField.dari(by).kolom()   // hanya id, nama, status yang lolos
```

Inilah alasan `SortField` berupa `enum`, bukan `String` yang disaring dengan regex.

### Validasi yang dipasang

| DTO | Aturan |
|---|---|
| `CreateKaryawanRequest`, `UpdateKaryawanRequest` | `nama` wajib & maks 100, `status` harus `AKTIF`/`NONAKTIF` |
| `DetailKaryawanRequest` | `nik` 16 digit, `npwp` 15 digit |
| `PayrollRequest` | `idKaryawan` & `periode` wajib, nominal tidak boleh negatif |
| `PayrollUpdateRequest` | nominal tidak boleh negatif |

`GlobalExceptionHandler` memetakan `MethodArgumentNotValidException` (body) dan `ConstraintViolationException` (query param) ke **400** dengan amplop yang sama seperti error lain, menyebut field mana yang bermasalah.

### Mencobanya

```bash
# RENTAN: bocor seluruh baris
curl -s 'localhost:8080/api/vuln/karyawan/search?nama=%27%20OR%20%271%27=%271' | jq length

# AMAN: payload sama, 0 baris - dianggap teks biasa
curl -s 'localhost:8080/api/safe/karyawan/search?nama=%27%20OR%20%271%27=%271' \
  -H "Authorization: Bearer $TOKEN" | jq length

# RENTAN: ekspresi SQL sembarang dieksekusi -> error division by zero dari database
curl -s 'localhost:8080/api/vuln/karyawan/sort?by=1/0'

# AMAN: ditolak allowlist -> 400
curl -s 'localhost:8080/api/safe/karyawan/sort?by=1/0' -H "Authorization: Bearer $TOKEN"
```

> Kode aplikasi yang sudah ada **memang sudah** memakai bind parameter dengan benar sejak awal. Controller rentan di `owasp/vuln` dibuat khusus untuk demonstrasi, bukan karena ada lubang nyata di sana.

---

## A03 — Injection — XSS

**Sederhananya:** user menyimpan kode program, lalu kode itu berjalan di browser orang lain.

Proyek ini REST/JSON murni tanpa halaman HTML, jadi yang relevan adalah **stored XSS**: payload disimpan di database sekarang, dan meledak nanti saat frontend menampilkannya.

Dua bentuk yang dibuat di sini:

- **Stored XSS** — `<script>alert(1)</script>` tersimpan di kolom `nama`.
- **Log injection** — user menyisipkan baris baru ke dalam input sehingga bisa memalsukan isi log.

| | Lokasi |
|---|---|
| Rentan | `owasp/vuln/A03XssVulnController.java` |
| Aman | `owasp/safe/A03XssSafeController.java` |
| Pendukung | `owasp/safe/InputSanitizer.java` |
| Test | `owasp/A03XssTest.java` — 6 test, semua lulus |

**Cara amannya:** tag HTML pada field teks bebas ditolak, header `X-Content-Type-Options: nosniff` dipasang, dan karakter `\r` `\n` dibuang sebelum input masuk log.

### Kenapa menolak, bukan meng-escape

API ini mengembalikan JSON, dan yang menampilkannya bisa siapa saja — halaman web, aplikasi mobile, laporan Excel. Kalau kita meng-escape untuk HTML di sisi server, klien non-HTML akan menerima `&lt;script&gt;` yang salah tampil. Karena `nama` dan `alamat` memang tidak pernah wajar memuat `<` atau `>`, menolaknya di gerbang masuk lebih sederhana dan tidak membebani klien.

### Log injection

Payload berisi baris baru bisa memalsukan baris log:

```
keyword=budi
WARN  Saldo kas berhasil ditransfer ke rekening penyerang
```

Bagi mata manusia dan bagi banyak parser log, baris kedua tampak seperti log asli dari aplikasi. Versi aman mengganti `\r` dan `\n` jadi `_` sehingga satu input tetap jadi satu baris:

```
keyword=budi_WARN  Saldo kas berhasil ditransfer ke rekening penyerang
```

### Mencobanya

```bash
# RENTAN: script tersimpan utuh dan dikembalikan apa adanya
curl -s -X POST localhost:8080/api/vuln/karyawan/teks \
  -H 'Content-Type: application/json' \
  -d '{"nama":"<script>alert(1)</script>","alamat":"Jakarta"}'

# AMAN: ditolak 400 sebelum menyentuh database
curl -s -X POST localhost:8080/api/safe/karyawan/teks \
  -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" \
  -d '{"nama":"<script>alert(1)</script>","alamat":"Jakarta"}'
```

> `X-Content-Type-Options: nosniff` sebenarnya sudah dipasang Spring Security secara bawaan. Test di sini memastikan header itu tidak hilang tanpa sengaja. Pengerasan header selengkapnya (CSP, HSTS) menyusul di [A05](#a05--security-misconfiguration).

---

## A04 — Insecure Design

**Sederhananya:** kodenya benar, **rancangannya** yang salah.

Beda dengan kategori lain yang bisa ditambal, kategori ini butuh perubahan cara berpikir. Tidak ada bug yang bisa ditunjuk — fiturnya memang dirancang tanpa memikirkan penyalahgunaan.

Dua bentuk yang dibuat di sini:

- **Tanpa pembatasan laju** — penyerang bisa mencoba ribuan password per menit karena memang tidak pernah dibatasi.
- **Aturan bisnis longgar** — slip gaji yang sudah disetujui masih bisa diubah diam-diam, dan periode masa depan diterima begitu saja.

| | Lokasi |
|---|---|
| Rentan | `owasp/vuln/A04VulnController.java` (revisi paksa), `owasp/vuln/A07VulnController.java` (login tanpa batas) |
| Aman | `owasp/safe/RateLimitFilter.java`, `entity/PayrollKaryawan.java`, `entity/StatusPayroll.java` |
| Migrasi | `payroll_karyawan_status_masesas.sql` |
| Test | `owasp/A04InsecureDesignTest.java` — 5 test, semua lulus |

**Cara amannya:** Bucket4j membatasi 10 permintaan per menit per IP, dan aturan bisnis ditegakkan di layer domain — slip berstatus `APPROVED` menolak revisi, periode masa depan ditolak.

### State machine slip gaji

```
DRAFT  --(POST .../approve)-->  APPROVED
  |                                 |
  +-- boleh direvisi                +-- revisi ditolak 422, selamanya
```

Aturannya hidup di `PayrollKaryawan.revisi()`, **bukan di controller**. Ini bedanya penting: kalau pengecekan ditaruh di controller, jalur lain (job terjadwal, importer batch, controller baru) akan melewatinya tanpa sadar. Di entity, tidak ada jalur yang bisa lolos.

Endpoint baru: `POST /api/payroll/{idKaryawan}/{periode}/approve` — butuh `ROLE_HR`.

> **Migrasi database diperlukan.** Kolom `status` ditambahkan lewat `payroll_karyawan_status_masesas.sql`. Skripnya aman dijalankan berulang dan bersifat menambah saja — baris lama otomatis `DRAFT`.

### Rate limiting

Path yang dibatasi 10 permintaan/menit per IP: `/api/safe/login` dan `/api/safe/karyawan/search`. Melebihi batas dibalas **429** dengan header `Retry-After: 60`. Atur lewat `app.rate-limit.per-minute`.

### Mencobanya

```bash
# AMAN: permintaan ke-11 dalam satu menit -> 429
for i in $(seq 1 11); do
  curl -s -o /dev/null -w "$i: %{http_code}\n" -X POST localhost:8080/api/safe/login \
    -H 'Content-Type: application/json' \
    -d '{"username":"hr","password":"Password123!"}'
done

# RENTAN: berapa kali pun tetap 200
for i in $(seq 1 15); do
  curl -s -o /dev/null -w "$i: %{http_code}\n" -X POST localhost:8080/api/vuln/login \
    -H 'Content-Type: application/json' \
    -d '{"username":"hr","password":"Password123!"}'
done
```

---

## A05 — Security Misconfiguration

**Sederhananya:** perangkat lunaknya aman, **pengaturannya** yang bocor.

Contoh khas: password ditulis langsung di file konfigurasi, mode debug menyala di produksi, pesan error menampilkan seluruh stacktrace.

Kategori ini tidak punya endpoint — bedanya ada di file konfigurasi.

Dua bentuk yang diperbaiki:

- **Kredensial hardcoded** — password database dan Redis tertulis langsung di `application.properties`.
- **Tanpa security header** — tidak ada HSTS, CSP, atau `nosniff`.

| | Lokasi |
|---|---|
| Sebelum | `src/main/resources/application.properties` |
| Sesudah | `.env`, `.env.example`, `.gitignore`, `config/SecurityConfig.java` |
| Test | `owasp/A05MisconfigTest.java` — 7 test, semua lulus |

**Cara amannya:** kredensial pindah ke `.env` yang tidak ikut di-commit, `.env.example` berisi placeholder untuk panduan, security header dipasang di `SecurityConfig`, `show-sql` dimatikan, dan stacktrace tidak pernah dikirim ke klien.

### Menjalankan setelah clone

```bash
cp .env.example .env
# isi nilainya, lalu:
./mvnw spring-boot:run
```

Tanpa `.env`, aplikasi **gagal start** dengan pesan placeholder tidak bisa di-resolve. Itu disengaja — tidak ada nilai bawaan yang diam-diam lolos ke lingkungan nyata.

Spring Boot tidak membaca `.env` secara bawaan. Yang membuatnya terbaca adalah satu baris di `application.properties`:

```properties
spring.config.import=optional:file:.env[.properties]
```

Penanda `[.properties]` memberi tahu Spring cara mem-parsing berkas tanpa ekstensi yang dikenal. Konsekuensinya `.env` mengikuti aturan `.properties`: **tanpa awalan `export`, tanpa tanda kutip** — tanda kutip akan ikut terbaca sebagai bagian dari nilai.

### Security header yang dipasang

| Header | Nilai | Gunanya |
|---|---|---|
| `Content-Security-Policy` | `default-src 'none'; frame-ancestors 'none'; base-uri 'none'` | API ini tidak pernah perlu memuat aset apa pun |
| `X-Frame-Options` | `DENY` | Cegah clickjacking |
| `X-Content-Type-Options` | `nosniff` | Browser tidak menebak-nebak tipe konten |
| `Referrer-Policy` | `no-referrer` | URL yang memuat ID tidak bocor ke situs lain |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | Hanya dikirim pada koneksi HTTPS |

### CORS

Origin ditulis eksplisit lewat `app.cors.allowed-origins`, **bukan** `*`. Dengan `allowCredentials` menyala, wildcard ditolak spesifikasi CORS — dan seandainya diizinkan pun, artinya situs mana pun boleh memanggil API ini memakai kredensial korban. Origin asing dibalas **403** pada preflight.

### Yang masih terbuka

| Hal | Status |
|---|---|
| Kredensial keluar dari `application.properties` | ✅ selesai |
| `JWT_SECRET` dan `CRYPTO_KEY` dibuat baru, tidak pernah ter-commit | ✅ selesai |
| Password DB/Redis lama masih ada di riwayat git repo publik | ❌ **belum dirotasi** |

Password `binar_bc_password` sudah terlanjur ter-push ke repo publik sejak commit pertama. Memindahkannya ke `.env` **tidak menghapus jejak itu**. Perbaikan sesungguhnya adalah rotasi, dan karena `binar_finance` dipakai beberapa peserta, rotasi perlu koordinasi dengan pengelolanya. Ditunda atas keputusan pemilik proyek.

---

## A06 — Vulnerable and Outdated Components

**Sederhananya:** kode kita aman, tapi library yang kita pakai punya lubang yang sudah diketahui publik.

Aplikasi modern memakai ratusan library, sebagian besar tidak pernah kita baca. Kalau salah satunya punya CVE dan kita tidak pernah memeriksanya, lubang itu jadi lubang kita juga.

Kategori ini tidak punya endpoint — bedanya ada di `pom.xml`.

Dua bentuk yang diperbaiki:

- **Tanpa pemindaian** — tidak ada yang memeriksa apakah dependency punya kerentanan.
- **Versi tidak dikunci** — versi dinamis membuat build tidak dapat diulang.

| | Lokasi |
|---|---|
| Sebelum & sesudah | `pom.xml` |
| Test | build gagal bila ditemukan CVSS ≥ 7 |

```bash
mvn verify              # pemindaian ikut berjalan
mvn dependency-check:check   # pemindaian saja
```

**Cara amannya:** `dependency-check-maven` memindai seluruh dependency terhadap basis data NVD dan menggagalkan build bila ada kerentanan CVSS ≥ 7; `maven-enforcer-plugin` melarang versi dinamis dan dependency SNAPSHOT.

> Pemindaian pertama mengunduh basis data NVD dan bisa memakan beberapa menit.

---

## A07 — Identification and Authentication Failures

**Sederhananya:** aplikasi tidak bisa memastikan kamu benar-benar dirimu.

Sementara A01 menjawab "apa kamu **boleh**?", A07 menjawab pertanyaan yang lebih dulu: "**siapa** kamu?". Karena itu A07 dikerjakan lebih dulu — tanpa jawaban ini, A01 tidak ada artinya.

Dua bentuk yang dibuat di sini:

- **Token tanpa masa berlaku** — sekali bocor, berlaku selamanya.
- **Percobaan login tak terbatas** — penyerang bisa menebak password sampai ketemu.

| | Lokasi |
|---|---|
| Rentan | `owasp/vuln/A07VulnController.java` |
| Aman | `owasp/safe/A07SafeController.java` |
| Pendukung | `security/JwtService.java`, `security/JwtAuthFilter.java`, `security/LoginAttemptService.java`, `security/AppUser.java`, `security/AppUserDetailsService.java` |
| Test | `owasp/A07AuthTest.java` — 5 test, semua lulus |

**Cara amannya:** JWT dengan masa berlaku 15 menit, password diverifikasi lewat bcrypt, dan akun dikunci 15 menit setelah 5 kali gagal (penghitung disimpan di Redis).

### Akun uji

| Username | Peran | `idKaryawan` |
|---|---|---|
| `admin` | `ROLE_ADMIN` | — |
| `hr` | `ROLE_HR` | — |
| `karyawan` | `ROLE_KARYAWAN` | 1 |

Password ketiganya sama, diambil dari properti `app.demo.password` (nilai awal `Password123!`).

### Mencobanya

```bash
# versi aman - token berlaku 15 menit
curl -s -X POST localhost:8080/api/safe/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"hr","password":"Password123!"}'

# versi rentan - token tanpa masa berlaku, berlaku selamanya
curl -s -X POST localhost:8080/api/vuln/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"hr","password":"Password123!"}'

# salah password 5x di /api/safe/login -> percobaan ke-6 dibalas 423 Locked
# salah password berapa kali pun di /api/vuln/login -> tidak pernah terkunci
```

Tempel `token` ke request berikutnya sebagai `Authorization: Bearer <token>`.

> `app.jwt.secret` dan `app.demo.password` masih tertulis di `application.properties`. Keduanya pindah ke `.env` pada Fase 3 (A05).

---

## A08 — Software and Data Integrity Failures

**Sederhananya:** data atau kode berubah tanpa terdeteksi.

Dua bentuk yang dibuat di sini:

- **Lost update** — dua orang HR mengubah slip gaji yang sama di saat bersamaan; perubahan pertama tertimpa tanpa peringatan.
- **Deserialisasi tak aman** — data dari Redis diubah jadi objek Java tanpa membatasi kelas apa yang boleh dibuat, sehingga penyerang yang menguasai Redis bisa menjalankan kode.

| | Lokasi |
|---|---|
| Rentan | `owasp/vuln/A08VulnController.java` |
| Aman | `controller/PayrollController.java`, `service/impl/PayrollServiceImpl.java` |
| Pendukung | `entity/PayrollKaryawan.java`, `config/RedisConfig.java`, `exception/ConflictException.java` |
| Migrasi | `payroll_karyawan_version_masesas.sql` |
| Test | `owasp/A08IntegrityTest.java` — 5 test, semua lulus |

**Cara amannya:** `@Version` (optimistic locking) membuat perubahan kedua ditolak dengan 409 alih-alih menimpa diam-diam, dan `RedisConfig` membatasi kelas yang boleh dideserialisasi lewat allowlist.

### `@Version` saja tidak cukup

Ini bagian yang paling mudah salah dipahami. `@Version` melindungi dua transaksi yang menulis **bersamaan**. Tapi API ini stateless — setiap request memuat ulang barisnya dari nol, jadi dua request berurutan tidak pernah bentrok di mata Hibernate:

```
request A: baca (v0) ... tulis 6jt -> v1     ✅
request B: baca (v1) ... tulis 7jt -> v2     ✅ padahal B mengedit layar lama
```

Yang hilang: B mengambil keputusan berdasarkan angka yang sudah basi. Solusinya **klien harus mengirim balik versi yang dia lihat** saat membaca:

```json
PUT /api/payroll/12/2026-08-01
{"version": 0, "gajiPokok": 7000000}
```

Kalau `version` yang dikirim tidak sama dengan versi tersimpan, dibalas **409**. Dua lapis yang saling melengkapi:

| Lapis | Menangkap |
|---|---|
| `version` di body request | Edit di atas data basi, walau jaraknya berjam-jam |
| `@Version` Hibernate | Dua transaksi menulis benar-benar bersamaan |

`version` bersifat opsional di request — dikirim `null` berarti melewati pengecekan lapis pertama. Ini menjaga pemanggil lama tetap jalan, tapi klien yang peduli konsistensi wajib mengirimkannya.

### Allowlist deserialisasi Redis

`RedisConfig` sudah memakai `BasicPolymorphicTypeValidator` sejak awal. Yang ditambahkan di sini: serializer-nya dipisah jadi bean tersendiri supaya **bisa diuji langsung**, plus test yang membuktikan `{"@class":"java.io.File",...}` ditolak. Tanpa test itu, allowlist bisa hilang saat refactor tanpa ada yang sadar sampai terlambat.

### Mencobanya

```bash
# AMAN: kirim versi yang benar -> 200, version naik jadi 1
curl -s -X PUT localhost:8080/api/payroll/12/2026-08-01 \
  -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" \
  -d '{"version":0,"gajiPokok":6000000}'

# AMAN: kirim versi basi -> 409
curl -s -X PUT localhost:8080/api/payroll/12/2026-08-01 \
  -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" \
  -d '{"version":0,"gajiPokok":7000000}'

# RENTAN: tanpa cek versi, penulisan terakhir selalu menang
curl -s -X PUT localhost:8080/api/vuln/payroll/12/2026-08-01/tanpa-versi \
  -H 'Content-Type: application/json' -d '{"version":0,"gajiPokok":7000000}'
```

---

## A09 — Security Logging and Monitoring Failures

**Sederhananya:** penyerang masuk, dan tidak ada satu pun jejak yang tertinggal.

Kategori ini tidak mencegah serangan — ia memastikan kamu **tahu** kalau diserang. Tanpa log, pelanggaran baru ketahuan berbulan-bulan kemudian.

Dua bentuk yang diperbaiki:

- **Tanpa jejak audit** — siapa mengubah gaji siapa, kapan, dari mana: tidak tercatat sama sekali.
- **Log yang membuang informasi** — `Karyawan2Service:64` hanya mencatat pesan error dan membuang stacktrace-nya.

| | Lokasi |
|---|---|
| Aman | `owasp/safe/AuditAspect.java`, `owasp/safe/AuditLogger.java`, `owasp/safe/CorrelationIdFilter.java` |
| Diperbaiki | `service/Karyawan2Service.java` |
| Test | `owasp/A09LoggingTest.java` — 6 test, semua lulus |

**Cara amannya:** setiap operasi tulis payroll menghasilkan satu baris audit berisi aktor, aksi, waktu, dan IP; setiap permintaan diberi correlation ID sehingga seluruh lognya bisa ditelusuri sebagai satu rangkaian; event login sukses dan gagal ikut dicatat.

### Bentuk baris audit

```
aksi=payroll.update sasaran=12,2026-08-01 aktor=hr peran=ROLE_HR ip=127.0.0.1 hasil=BERHASIL
aksi=auth.login     sasaran=hr            aktor=anonim peran=- ip=127.0.0.1 hasil=GAGAL:KREDENSIAL
```

Empat keputusan di baliknya:

| Keputusan | Alasan |
|---|---|
| Logger bernama `AUDIT`, bukan nama kelas | Jejak audit punya pembaca dan masa simpan berbeda dari log debug, jadi harus bisa dipisah ke appender sendiri |
| Aspect ditempel di **service**, bukan controller | Jalur lain yang memanggil service langsung — job terjadwal, importer, controller baru — tetap tercatat |
| Operasi **gagal** ikut dicatat | Percobaan yang ditolak justru sinyal serangan paling berharga. Log yang hanya mencatat keberhasilan buta terhadap penyusup yang sedang meraba-raba |
| Hanya argumen bertipe sederhana yang dicatat | Objek request lengkap memuat nominal gaji. Jejak audit tidak boleh jadi tempat bocornya data yang justru sedang dilindungi |

### Correlation ID

Setiap request dapat `X-Correlation-Id` — dipakai ulang kalau klien mengirimnya, dibuatkan baru kalau tidak. Nilainya masuk MDC sehingga muncul di **setiap** baris log request itu:

```properties
logging.pattern.level=%5p [%X{correlationId:-tanpa-id}]
```

Nilai dari klien **tidak pernah dipercaya mentah**. Ia masuk ke setiap baris log, jadi baris baru di dalamnya bisa memalsukan entri log — persis serangan yang sama seperti di [A03 XSS](#a03--injection--xss). Karena itu disanitasi lewat `InputSanitizer.untukLog()` dan dipotong di 64 karakter supaya satu request tidak bisa membanjiri berkas log.

### Yang diperbaiki

`Karyawan2Service` sebelumnya menulis:

```java
log.error("this is error message {}", e.getMessage());
throw new RuntimeException("ini adalah error method page di dalam service karyawan");
```

Dua masalah: `e.getMessage()` membuang stacktrace sehingga asal masalahnya hilang, dan membungkus ulang jadi `RuntimeException` generik menghapus tipe exception aslinya. Sekarang exception-nya diteruskan sebagai argumen terakhir tanpa placeholder — cara SLF4J mencetak stacktrace penuh — dan exception aslinya dilempar ulang apa adanya.

---

## A10 — Server-Side Request Forgery (SSRF)

**Sederhananya:** penyerang menyuruh **server kita** menembak alamat yang dia mau.

Kenapa berbahaya: server kita berada di dalam jaringan internal. Alamat yang tidak bisa dijangkau penyerang dari luar — Redis, database, endpoint metadata cloud — bisa dijangkau oleh server kita. Penyerang cukup meminjam tangan kita.

Fiturnya: mengambil foto profil karyawan dari URL yang diberikan user.

Dua bentuk yang dibuat di sini:

- **URL tidak divalidasi** — `http://127.0.0.1:6379` atau `http://169.254.169.254/latest/meta-data/` diterima begitu saja.
- **Redirect diikuti, tanpa timeout** — validasi bisa dilewati dengan URL yang mengarahkan ulang ke alamat internal.

| | Lokasi |
|---|---|
| Rentan | `owasp/vuln/A10VulnController.java` |
| Aman | `owasp/safe/A10SafeController.java` |
| Pendukung | `owasp/safe/UrlGuard.java`, `config/ClientConfig.java` |
| Test | `owasp/A10SsrfTest.java` |

**Cara amannya:** hanya skema `https` yang diterima, nama host di-resolve lebih dulu lalu IP privat/loopback/link-local ditolak, redirect tidak diikuti, timeout 3 detik, ukuran dibatasi 2 MB, dan `Content-Type` wajib `image/*`.

---

## Ringkasan berkas

```
demo1/
├── TOP10-OWASP.md              dokumen ini
├── IMPLEMENTATION-PLAN.md      cara membangunnya
├── .env.example                contoh kredensial (ikut di-commit)
├── .env                        kredensial asli (TIDAK ikut di-commit)
├── pom.xml                     A06
└── src/
    ├── main/java/com/masesas/exercises/demo1/
    │   ├── security/           infrastruktur JWT (A07)
    │   ├── owasp/vuln/         semua versi rentan
    │   ├── owasp/safe/         semua versi aman
    │   └── config/             SecurityConfig, RedisConfig, ClientConfig
    ├── main/resources/
    │   └── application.properties
    └── test/java/com/masesas/exercises/demo1/owasp/
        └── A01..A10 test
```
