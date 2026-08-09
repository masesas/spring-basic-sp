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
| [A01](#a01--broken-access-control) | Broken Access Control | `/api/vuln/payroll/{id}` | `/api/safe/payroll/{id}` | ⬜ Belum |
| [A02](#a02--cryptographic-failures) | Cryptographic Failures | `/api/vuln/karyawan/{id}/detail` | `/api/safe/karyawan/{id}/detail` | ⬜ Belum |
| [A03-SQL](#a03--injection--sql) | Injection — SQL | `/api/vuln/karyawan/search` | `/api/safe/karyawan/search` | ⬜ Belum |
| [A03-XSS](#a03--injection--xss) | Injection — XSS | `POST /api/vuln/karyawan` | `POST /api/safe/karyawan` | ⬜ Belum |
| [A04](#a04--insecure-design) | Insecure Design | `/api/vuln/login` | `/api/safe/login` | ⬜ Belum |
| [A05](#a05--security-misconfiguration) | Security Misconfiguration | — *(konfigurasi)* | — *(konfigurasi)* | ⬜ Belum |
| [A06](#a06--vulnerable-and-outdated-components) | Vulnerable & Outdated Components | — *(pom.xml)* | — *(pom.xml)* | ⬜ Belum |
| [A07](#a07--identification-and-authentication-failures) | Identification & Authentication Failures | `POST /api/vuln/login` | `POST /api/safe/login` | ⬜ Belum |
| [A08](#a08--software-and-data-integrity-failures) | Software & Data Integrity Failures | `PUT /api/vuln/payroll` | `PUT /api/safe/payroll` | ⬜ Belum |
| [A09](#a09--security-logging-and-monitoring-failures) | Security Logging & Monitoring Failures | — *(aspect)* | — *(aspect)* | ⬜ Belum |
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
| Pendukung | `config/SecurityConfig.java` |
| Test | `owasp/A01AccessControlTest.java` |

**Cara amannya:** aturan per-endpoint di `SecurityConfig` ditambah `@PreAuthorize` di tiap method. Untuk IDOR, dicek bahwa ID yang diminta memang milik si pengguna.

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
| Pendukung | `owasp/safe/CryptoConverter.java`, `entity/DetailKaryawan.java` |
| Test | `owasp/A02CryptoTest.java` |

**Cara amannya:** NIK dan NPWP dienkripsi AES-GCM otomatis saat masuk database (lewat `AttributeConverter`), password pakai bcrypt, dan response hanya menampilkan versi tersamar `************1234`.

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
| Pendukung | `owasp/safe/SortField.java`, anotasi validasi di seluruh `dto/` |
| Test | `owasp/A03SqlInjectionTest.java` |

**Cara amannya:** semua nilai lewat bind parameter `?`, nama kolom sort dibatasi daftar tetap (`enum SortField`), dan seluruh input divalidasi lebih dulu dengan `@Valid`.

> Catatan: kode yang ada sekarang **sudah** memakai bind parameter dengan benar. Versi rentan dibuat khusus untuk demonstrasi.

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
| Test | `owasp/A03XssTest.java` |

**Cara amannya:** tag HTML pada field teks bebas ditolak, header `X-Content-Type-Options: nosniff` dipasang, dan karakter `\r` `\n` dibuang sebelum input masuk log.

---

## A04 — Insecure Design

**Sederhananya:** kodenya benar, **rancangannya** yang salah.

Beda dengan kategori lain yang bisa ditambal, kategori ini butuh perubahan cara berpikir. Tidak ada bug yang bisa ditunjuk — fiturnya memang dirancang tanpa memikirkan penyalahgunaan.

Dua bentuk yang dibuat di sini:

- **Tanpa pembatasan laju** — penyerang bisa mencoba ribuan password per menit karena memang tidak pernah dibatasi.
- **Aturan bisnis longgar** — slip gaji yang sudah disetujui masih bisa diubah diam-diam, dan periode masa depan diterima begitu saja.

| | Lokasi |
|---|---|
| Rentan | `owasp/vuln/A04VulnController.java` |
| Aman | `owasp/safe/RateLimitFilter.java`, `service/impl/PayrollServiceImpl.java` |
| Test | `owasp/A04InsecureDesignTest.java` |

**Cara amannya:** Bucket4j membatasi 10 permintaan per menit per IP, dan aturan bisnis ditegakkan di layer domain — slip berstatus `APPROVED` menolak revisi, periode masa depan ditolak.

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
| Test | `owasp/A05MisconfigTest.java` |

**Cara amannya:** kredensial pindah ke `.env` yang tidak ikut di-commit, `.env.example` berisi placeholder untuk panduan, security header dipasang di `SecurityConfig`, `show-sql` dimatikan, dan stacktrace tidak pernah dikirim ke klien.

> **Belum selesai:** password lama sudah terlanjur ter-commit ke repo publik dan **harus dirotasi**. Menghapus git history saja tidak cukup. Lihat bagian 8 di [IMPLEMENTATION-PLAN.md](./IMPLEMENTATION-PLAN.md).

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
| Pendukung | `security/JwtService.java`, `security/JwtAuthFilter.java`, `security/LoginAttemptService.java` |
| Test | `owasp/A07AuthTest.java` |

**Cara amannya:** JWT dengan masa berlaku 15 menit, password diverifikasi lewat bcrypt, dan akun dikunci 15 menit setelah 5 kali gagal (penghitung disimpan di Redis).

Tiga akun uji tersedia: `admin`, `hr`, `karyawan`.

---

## A08 — Software and Data Integrity Failures

**Sederhananya:** data atau kode berubah tanpa terdeteksi.

Dua bentuk yang dibuat di sini:

- **Lost update** — dua orang HR mengubah slip gaji yang sama di saat bersamaan; perubahan pertama tertimpa tanpa peringatan.
- **Deserialisasi tak aman** — data dari Redis diubah jadi objek Java tanpa membatasi kelas apa yang boleh dibuat, sehingga penyerang yang menguasai Redis bisa menjalankan kode.

| | Lokasi |
|---|---|
| Rentan | `owasp/vuln/A08VulnController.java` |
| Aman | `owasp/safe/A08SafeController.java` |
| Pendukung | `entity/PayrollKaryawan.java`, `config/RedisConfig.java` |
| Test | `owasp/A08IntegrityTest.java` |

**Cara amannya:** `@Version` (optimistic locking) membuat perubahan kedua ditolak dengan 409 alih-alih menimpa diam-diam, dan `RedisConfig` membatasi kelas yang boleh dideserialisasi lewat allowlist.

> `RedisConfig` **sudah** memakai `BasicPolymorphicTypeValidator` dengan benar. Tugas di sini adalah mempertahankannya dan menambah test supaya tidak hilang tanpa sengaja.

---

## A09 — Security Logging and Monitoring Failures

**Sederhananya:** penyerang masuk, dan tidak ada satu pun jejak yang tertinggal.

Kategori ini tidak mencegah serangan — ia memastikan kamu **tahu** kalau diserang. Tanpa log, pelanggaran baru ketahuan berbulan-bulan kemudian.

Dua bentuk yang diperbaiki:

- **Tanpa jejak audit** — siapa mengubah gaji siapa, kapan, dari mana: tidak tercatat sama sekali.
- **Log yang membuang informasi** — `Karyawan2Service:64` hanya mencatat pesan error dan membuang stacktrace-nya.

| | Lokasi |
|---|---|
| Aman | `owasp/safe/AuditAspect.java`, `owasp/safe/CorrelationIdFilter.java` |
| Diperbaiki | `service/Karyawan2Service.java` |
| Test | `owasp/A09LoggingTest.java` |

**Cara amannya:** setiap operasi tulis payroll menghasilkan satu baris audit berisi aktor, aksi, waktu, dan IP; setiap permintaan diberi correlation ID sehingga seluruh lognya bisa ditelusuri sebagai satu rangkaian; event login sukses dan gagal ikut dicatat.

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
