# Ringkasan Pekerjaan — OWASP Top 10:2021

Penerapan 10 kategori OWASP Top 10:2021 pada proyek `demo1` (Spring Boot 4.1, Java 21).

Dokumen ini adalah ringkasan tingkat tinggi: apa yang dikerjakan, apa yang ditemukan, dan apa yang belum tuntas.

---

## Dokumen rujukan

| Berkas | Isi |
|---|---|
| **[TOP10-OWASP.md](./TOP10-OWASP.md)** | **Titik masuk utama.** Penjelasan tiap kategori dengan bahasa awam, plus tabel indeks yang memetakan setiap kategori ke endpoint rentan, endpoint aman, berkas kode, dan test-nya. Dilengkapi contoh `curl` per kategori |
| [IMPLEMENTATION-PLAN.md](./IMPLEMENTATION-PLAN.md) | Cara membangunnya: keputusan desain, dependency, struktur folder, empat fase pengerjaan, batasan yang diketahui |

Kalau ada yang bertanya *"A05 itu apa dan ada di mana?"*, jawabannya satu baris: buka `TOP10-OWASP.md`, lihat tabel indeks.

---

## Cakupan

**10 dari 10 kategori terimplementasi.** Setiap kategori punya dua versi:

- **rentan** — di bawah `/api/vuln/**`, dipagari `@Profile("owasp-demo")` sehingga tidak pernah aktif kecuali dinyalakan sengaja
- **aman** — versi yang benar

Test menjalankan **serangan yang sama** ke keduanya: harus berhasil di yang rentan, gagal di yang aman.

| Fase | Kategori | Status |
|---|---|---|
| 0 | Perbaikan baseline | ✅ |
| 1 | A07 Authentication · A01 Access Control | ✅ |
| 2 | A03 Injection SQL · A03 Injection XSS · A04 Insecure Design | ✅ |
| 3 | A02 Cryptographic · A05 Misconfiguration · A08 Integrity | ✅ |
| 4 | A09 Logging · A10 SSRF · A06 Components | ✅ |

**Angka:** 15 commit · test tumbuh dari **25 → 90** · 8 controller rentan · 6 controller aman + 9 kelas pendukung · 5 kelas infrastruktur auth · 11 kelas test · 3 migrasi SQL (sudah diterapkan)

---

## Status akhir

```
$ ./mvnw verify

[INFO] Rule 0: BanDynamicVersions ......... passed
[INFO] Rule 1: RequireReleaseDeps ......... passed
[INFO] Rule 2: RequireJavaVersion ......... passed
[INFO] Tests run: 90, Failures: 0, Errors: 0, Skipped: 0
[INFO] dependency-check ................... 0 CVE >= 7.0
[INFO] BUILD SUCCESS
[INFO] Total time: 45.243 s
```

| | |
|---|---|
| Kategori OWASP terimplementasi | **10 / 10** |
| Test | **90 lulus**, 0 gagal |
| Gerbang build | enforcer ✅ · test ✅ · pemindaian kerentanan ✅ |
| Commit | 15, seluruhnya sudah di-push ke `origin/main` |
| Migrasi database | 3, sudah diterapkan dan diverifikasi |

Tidak ada pekerjaan yang tertahan setengah jalan. Yang tersisa di bawah adalah hal-hal yang memang **di luar kendali repo ini** atau **terjadwal untuk ditinjau nanti** — bukan bagian yang belum dikerjakan.

---

## Menjalankannya

```bash
cp .env.example .env      # isi nilainya
./mvnw spring-boot:run
```

Tanpa `.env`, aplikasi **gagal start** — disengaja, supaya tidak ada kredensial bawaan yang diam-diam lolos.

```bash
# menyalakan endpoint rentan (hanya di komputer sendiri)
./mvnw spring-boot:run -Dspring-boot.run.profiles=owasp-demo

# seluruh test
./mvnw test

# pemindaian kerentanan dependency (A06)
./mvnw verify -Dnvd.api.key=ISI_API_KEY_ANDA
```

> ⚠️ **Jangan pernah men-deploy dengan profil `owasp-demo` menyala.** Package `owasp/vuln` berisi kerentanan sungguhan, bukan simulasi.

Akun uji: `admin`, `hr`, `karyawan` — password diambil dari `DEMO_PASSWORD` di `.env`.

---

## Temuan terpenting

### Baseline ternyata rusak total

Sebelum menyentuh OWASP, proyek **tidak bisa di-compile sama sekali**. Lima kerusakan bawaan dari sesi sebelumnya:

1. `PayrollKaryawanRepository` tidak pernah ada, padahal di-import dan dipakai 8 kali
2. `PayrollServiceImpl.create()` memakai variabel yang tidak pernah dideklarasikan
3. `PayrollServiceImpl.findById()` tanpa `return` — kedua barisnya dikomentari
4. `JdbcConfig` membuat `new JdbcTemplate()` tanpa DataSource → seluruh ApplicationContext gagal dimuat
5. `KaryawanResponse` diubah dari `record` jadi kelas Lombok, menghapus `from()` dan merusak 5 test

Efek samping yang ikut terperbaiki: `/api/karyawan/search` sebelumnya **selalu mengembalikan data serba null** karena mapping-nya berupa stub kosong.

### Pelajaran yang hanya muncul karena test menjalankan serangan sungguhan

| Temuan | Kenapa penting |
|---|---|
| `@PreAuthorize` membalas **500, bukan 403** | `GlobalExceptionHandler` menelan `AccessDeniedException`, sehingga penolakan otorisasi tampak seperti kerusakan server — dan menyembunyikan apakah kontrol aksesnya benar-benar bekerja |
| `@Version` **tidak cukup** untuk API stateless | Setiap request memuat ulang barisnya, jadi Hibernate tidak pernah melihat konflik. Klien harus mengirim balik versi yang dilihat |
| `ORDER BY` **tidak bisa** diamankan bind parameter | `?` hanya menggantikan nilai, bukan nama kolom. Satu-satunya cara aman adalah allowlist |
| `save()` belum flush | `@Version` di response masih versi lama, sehingga klien mengirimkannya kembali sebagai versi basi |
| Kontrol keamanan yang bekerja **punya state** | Lockout Redis dan kuota rate limit bocor antar kelas test. Kalau test lulus tanpa perlu pembersihan, justru patut dicurigai kontrolnya tidak aktif |
| Serangan yang sama muncul di tempat berbeda | `InputSanitizer` yang dibuat untuk log injection di A03 langsung terpakai lagi di correlation ID A09 |

### 20 kerentanan yang selama ini tidak terlihat

Pemindaian A06 pertama menemukan **20 CVE dengan CVSS ≥ 7** — semuanya pada dependency bawaan `spring-boot-starter-parent:4.1.0`, bukan yang ditambahkan untuk pekerjaan ini. Termasuk **4 CVE skor 9.1 di Tomcat**, komponen yang menangani setiap request masuk.

| Dependency | Sebelum | Sesudah | Cara |
|---|---|---|---|
| `netty-transport` | 12 CVE, tertinggi 9.1 | **0** | 4.2.15 → 4.2.17.Final |
| `tomcat-embed-*` | 6 CVE, 9.1 ×4 | 1 tersisa | 11.0.22 → 11.0.24 |
| `postgresql` | 1 CVE (8.2) | **0** | 42.7.11 → 42.7.13 |
| `spring-boot-devtools` | 1 CVE (9.8) | **0** | false positive, di-suppress |
| `angus-activation` | 1 CVE (7.5) | 1 tersisa | sudah versi stabil terbaru |

**18 dari 20 benar-benar diperbaiki** dengan menaikkan versi, bukan disembunyikan. 90 test tetap lulus setelahnya.

Satu keputusan yang layak dicatat: analyzer **OSS Index dimatikan** karena membalas `401` untuk akses anonim. Build yang gagal karena sebab di luar kendali kita melatih orang untuk mengabaikan kegagalannya — jauh lebih berbahaya daripada kehilangan satu sumber data sekunder. NVD tetap sumber utama.

---

## Yang tersisa

### 1. Kredensial lama masih di riwayat git repo publik

`binar_bc_password` ada di `github.com/masesas/spring-basic-sp` sejak commit pertama. `.env` memindahkan kredensial keluar dari kode, tapi **tidak menghapus jejak itu** — perbaikan sesungguhnya adalah rotasi.

Kunci JWT dan kunci enkripsi sudah dibuat baru dan tidak pernah ter-commit. Password DB dan Redis menunggu rotasi; karena `binar_finance` dipakai beberapa peserta, rotasi perlu koordinasi dengan pengelolanya. Ditunda atas keputusan pemilik proyek.

### 2. Satu test flaky belum terjelaskan

`A05MisconfigTest.headerKeamananTerpasang` gagal sekali dengan 500 pada `/api/karyawan/all`, tidak bisa direproduksi di lima run berikutnya.

Sudah disingkirkan sebagai penyebab: kegagalan dekripsi — seluruh 1000 baris `detail_karyawan` terverifikasi terbaca kembali. Tersisa dugaan entri cache Redis basi atau gangguan koneksi sesaat ke server bersama.

### 3. Dua suppression A06 terjadwal ditinjau 2026-11-09

Bukan pekerjaan yang tertunda — build sudah hijau. Ini utang yang sengaja dijadwalkan.

| CVE | Dependency | Skor | Kenapa belum diperbaiki |
|---|---|---|---|
| CVE-2026-66299 | `tomcat-embed-*` 11.0.24 | 7.5 | Belum ada patch; 11.0.24 rilis terbaru |
| CVE-2025-7962 | `angus-activation` 2.0.3 | 7.5 | Belum ada patch; transitif dari Hibernate → JAXB |

Keduanya tercatat di `dependency-check-suppressions.xml` sebagai **risiko yang diterima sadar**, bukan false positive — dibedakan tegas dari CVE-2022-31691 pada devtools yang memang salah cocok. Setiap entri memuat alasan, tanggal keputusan, tanggal tinjau ulang, dan langkah konkret saat ditinjau.

### 4. Tiga properti versi yang harus dihapus nanti

`netty.version`, `tomcat.version`, dan `postgresql.version` di `pom.xml` menimpa bawaan Spring Boot untuk menutup 18 CVE. **Hapus ketiganya** begitu Spring Boot merilis parent yang membawa versi sama atau lebih baru — kalau tidak, justru menahan proyek di versi lama. Sudah ditulis sebagai komentar di `pom.xml`.

---

## Batasan yang diketahui

| Batasan | Keterangan |
|---|---|
| **DNS rebinding** (A10) | Ada jeda antara host di-resolve untuk diperiksa dan koneksi dibuat. Penutupnya adalah koneksi ke IP terverifikasi, bukan nama host — di luar cakupan v1 |
| **Jalur sukses A10 belum diuji end-to-end** | Test hanya bisa menjalankan server di 127.0.0.1, yang justru selalu ditolak `UrlGuard`. Aturan hostnya diuji terpisah sebagai unit test |
| **Command Injection dilewati** | Tidak ada alasan wajar sebuah HR API memanggil shell. Membuat fiturnya hanya untuk demo justru menambah risiko nyata |
| **Reflected XSS di luar cakupan** | Proyek ini REST/JSON murni tanpa render HTML; yang relevan hanya stored XSS |
| **Akun in-memory** | Tidak ada tabel user/role, tidak ada refresh token — hanya access token 15 menit |
| **Kunci enkripsi hilang = data hilang** | `app.crypto.key` satu-satunya cara membaca NIK dan NPWP. `VerifikasiDekripsiTest` dipasang sebagai penjaga permanen terhadap penggantian kunci tanpa enkripsi ulang |
| **A09 berhenti di log terstruktur** | Integrasi SIEM, alerting, dan dashboard di luar cakupan |

---

## Langkah berikutnya

| # | Tindakan | Kapan |
|---|---|---|
| 1 | **Rotasi kredensial DB dan Redis** — koordinasikan dengan pengelola `binar_finance` | Sesegera mungkin; ini satu-satunya lubang yang masih terbuka |
| 2 | Ambil **API key NVD gratis** kalau A06 masuk CI | Sebelum memasang CI |
| 3 | **Tinjau ulang dua suppression A06** | 2026-11-09, atau lebih awal kalau patch upstream rilis |
| 4 | **Hapus tiga properti versi** di `pom.xml` | Saat Spring Boot merilis parent dengan versi setara |
| 5 | Selidiki **test flaky `A05MisconfigTest`** | Kalau muncul lagi; stack trace tercatat penuh di log |
