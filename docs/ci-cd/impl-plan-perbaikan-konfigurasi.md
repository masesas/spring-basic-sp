# Impl Plan — Perbaikan Konfigurasi (Prasyarat)

Tiga cacat yang ditemukan saat analisa harus diperbaiki sebelum satu baris pun
workflow ditulis. Selama cacat ini ada, CI akan merah karena alasan yang tidak
ada hubungannya dengan CI, dan container di server akan gagal membaca data
terenkripsi.

Dokumen ini tidak menyentuh Docker maupun GitHub Actions. Isinya murni perbaikan
konfigurasi aplikasi yang sudah ada.

---

## 1. Sintaks placeholder default salah di `application.properties`

**Kondisi sekarang** (perubahan belum di-commit, terlihat di `git diff`):

```properties
spring.datasource.hikari.schema=${DB_SCHEMA:-public}
spring.jpa.properties.hibernate.default_schema=${DB_SCHEMA:-public}
spring.data.redis.password=${REDIS_PASSWORD:}
app.security.jwt-secret=${JWT_SECRET:-this-is-not-a-secure-key-please-change-it}
app.security.password=${DEMO_PASSWORD:-password123}
app.security.crypto-key=${CRYPTO_KEY:-this-is-not-a-secure-key-please-change-it}
```

**Masalahnya.** Spring memakai sintaks `${VAR:default}` dengan satu titik dua.
`:-` adalah sintaks shell, bukan Spring. Akibatnya tanda minus ikut terbaca
sebagai karakter pertama nilai default:

| Placeholder | Nilai kalau env kosong | Yang diharapkan |
|---|---|---|
| `${DB_SCHEMA:-public}` | `-public` | `public` |
| `${DEMO_PASSWORD:-password123}` | `-password123` | `password123` |
| `${CRYPTO_KEY:-this-is-...}` | `-this-is-...` | `this-is-...` |

Schema `-public` tidak ada di PostgreSQL, dan `-password123` tidak cocok dengan
hash bcrypt akun demo. Ini penyebab kegagalan test yang tercatat di sesi
sebelumnya: 4 kegagalan karena password tidak cocok, 6 karena kunci kripto.

**Keputusan: kredensial tidak diberi nilai default sama sekali.**

Komentar asli di berkas itu sudah menyatakan alasannya dan alasan itu masih benar
— lebih baik aplikasi menolak start daripada diam-diam hidup memakai kunci
bawaan yang bocor di source code. Nilai default hanya boleh untuk hal yang bukan
kredensial dan memang punya nilai wajar.

**Perubahan yang dilakukan** — kembalikan enam baris di atas ke bentuk wajib:

```properties
spring.datasource.hikari.schema=${DB_SCHEMA}
spring.jpa.properties.hibernate.default_schema=${DB_SCHEMA}
spring.data.redis.host=${REDIS_HOST}
spring.data.redis.port=${REDIS_PORT}
spring.data.redis.username=${REDIS_USERNAME}
spring.data.redis.password=${REDIS_PASSWORD}
app.security.jwt-secret=${JWT_SECRET}
app.security.password=${DEMO_PASSWORD}
app.security.crypto-key=${CRYPTO_KEY}
spring.datasource.url=${DB_URL}
```

Baris `spring.datasource.url` kehilangan nilai contohnya
(`jdbc:postgresql://host:5432/nama_database`) dengan alasan yang sama: nilai
contoh yang tidak valid hanya menunda kegagalan sampai koneksi pertama, bukan
mencegahnya.

**Empat baris `app.redis.*` ikut dijadikan wajib**, dan ini yang paling penting
dari seluruh daftar di atas:

```properties
app.redis.host=${REDIS_HOST}
app.redis.port=${REDIS_PORT}
app.redis.username=${REDIS_USERNAME}
app.redis.password=${REDIS_PASSWORD}
```

Sebelumnya keempatnya bernilai default `redis.mnet.web.id`, `6379`, `deployer`,
dan kosong. Yang membuatnya berbahaya bukan sekadar melanggar aturan di atas,
tapi kenyataan bahwa **cabang inilah yang benar-benar dipakai**:
`RedisConfig.java` membangun `RedisConnectionFactory` sendiri dari
`app.redis.*` lewat `AppConfigProperties`, sehingga `spring.data.redis.*` tidak
menentukan koneksi apa pun. Dengan default itu, `REDIS_HOST` yang hilang tidak
membuat aplikasi gagal — ia diam-diam menyambung ke host yang sama sekali
berbeda dari yang dimaksud.

Nilai default hanya tersisa di `application-local.properties`, dan hanya untuk
tiga placeholder non-kredensial yang punya nilai wajar di mesin pengembang:

```properties
app.security.cors-allowed-origins=${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}
app.dummy-json.base-url=${DUMMYJSON_BASE_URL:https://dummyjson.com}
app.image.base-dir=${APP_IMAGE_BASE_DIR:./resources/images}
```

Di `application-dev.properties` dan `application-prod.properties` ketiganya
wajib tanpa default: variabel yang hilang harus menghentikan container, bukan
diam-diam diganti nilai bawaan.

Nama bersisipan `APP_` mengikuti inventaris di
[impl-plan-env-github.md](./impl-plan-env-github.md) — container mengisinya dari
`.env.app` dengan nama itu. Nama yang tidak cocok membuat container diam-diam
memakai nilai default alih-alih nilai dari GitHub.

**Placeholder wajib saja tidak cukup.** Properti di bawah prefix `app` di-bind
lewat `@ConfigurationProperties`, dan Binder Spring mengabaikan placeholder yang
gagal resolve — nilainya menjadi string literal `${REDIS_HOST}`, yang tidak
kosong sehingga lolos `@NotBlank`. Penanganannya ada di
[impl-plan-application-properties-profiles.md](./impl-plan-application-properties-profiles.md),
bagian Fail-Fast.

---

## 2. `CRYPTO_KEY_DEV` bukan Base64

**Kondisi sekarang** di `.env.server.dev`:

```
CRYPTO_KEY_DEV=b0a11b094b8b0ea7360bf874c5f4f30672e0dafa1415f4339f792d90ea7c4b80
```

**Masalahnya.** `security/CryptoConverter.java` baris 29 melakukan:

```java
this.kunci = new SecretKeySpec(Base64.getDecoder().decode(kunciBase64), "AES");
```

Nilai di atas adalah heksadesimal 64 karakter. Di-decode sebagai Base64 hasilnya
48 byte, dan AES hanya menerima 16, 24, atau 32 byte. Aplikasi akan melempar
`InvalidKeyException` begitu kolom `nik` atau `npwp` dibaca atau ditulis.

**Ada masalah kedua yang lebih besar.** Data `nik` dan `npwp` di database sudah
terenkripsi memakai kunci yang ada di `.env` lokal:

```
CRYPTO_KEY=np1iEu6vzSPjj+5KveQmFWE+zQ/bM3DuNJKDCkVKYZY=
```

Karena dev, prod, dan pengembangan lokal menunjuk database yang sama, kunci ini
tidak boleh berbeda di manapun. Mengganti kunci membuat seluruh NIK dan NPWP yang
sudah tersimpan tidak bisa didekripsi lagi — tidak ada jalan kembali tanpa
migrasi dekripsi terlebih dahulu.

**Perubahan yang dilakukan.** `CRYPTO_KEY_DEV` dan `CRYPTO_KEY_PROD` diisi nilai
Base64 yang sama persis dengan `.env` lokal. Nilai ini tidak boleh diubah selama
tabel `detail_karyawan` masih berisi data terenkripsi.

---

## 3. `JWT_SECRET_DEV` memakai tanda kutip dan karakter khusus

**Kondisi sekarang:**

```
JWT_SECRET_DEV="KOMGR!Dl:vsBk9piK)6bcBk$hP^q>,G]*OR{&^ydT!Y"
```

**Masalahnya.** Tiga hal sekaligus:

1. Berkas dibaca sebagai `.properties` (`spring.config.import=...[.properties]`).
   Format properties tidak mengenal tanda kutip sebagai pembungkus — dua tanda
   kutip itu ikut menjadi bagian nilai kunci JWT.
2. `$` berpotensi diinterpolasi Docker Compose kalau nilai ini pernah lewat
   berkas yang diinterpolasi.
3. Karakter `:` di dalam nilai properties adalah pemisah key-value, yang membuat
   nilainya bergantung pada urutan parsing.

**Perubahan yang dilakukan.** Regenerate memakai alfabet Base64 saja:

```bash
openssl rand -base64 48
```

Berbeda dengan `CRYPTO_KEY`, mengganti `JWT_SECRET` aman — dampaknya hanya token
yang sedang beredar jadi tidak valid, dan TTL-nya cuma 15 menit.

---

## 4. `.env.server.prod` adalah salinan mentah `.env.server.dev`

Seluruh key di dalamnya masih bersuffix `_DEV`, termasuk
`SPRING_PROFILES_ACTIVE_DEV=dev`. Tidak ada satu pun key `_PROD`. Perbaikannya
dibahas di [impl-plan-env-github.md](./impl-plan-env-github.md), yang menulis
ulang kedua berkas dari nol.

---

## Checklist

- [ ] Enam placeholder `:-` di `application.properties` dikembalikan ke bentuk wajib `${VAR}`
- [ ] `spring.datasource.url` tidak lagi punya nilai contoh
- [ ] `app.redis.host`, `port`, `username`, `password` tidak lagi punya nilai default
- [ ] Tidak ada satu pun placeholder kredensial yang punya default di seluruh berkas
- [ ] `AppConfigProperties` menolak nilai yang masih berbentuk `${...}` saat start
- [ ] Placeholder non-kredensial tetap punya default dengan sintaks `${VAR:default}` yang benar
- [ ] Nama placeholder non-kredensial cocok dengan inventaris (`APP_IMAGE_BASE_DIR`, `APP_REDIS_KEY_PREFIX`, `APP_CORS_ALLOWED_ORIGINS`)
- [ ] `CRYPTO_KEY` di `.env` lokal, `.env.server.dev`, dan `.env.server.prod` bernilai identik dan valid Base64 32 byte
- [ ] `JWT_SECRET` diregenerate tanpa tanda kutip dan tanpa karakter di luar alfabet Base64
- [ ] `./mvnw clean test` hijau dengan `.env` lokal terpasang

## Test Verifikasi

Perbaikan ini tidak menambah kode, jadi tidak ada test baru. Verifikasinya adalah
test suite yang sudah ada harus hijau:

```bash
./mvnw clean test
```

Yang membuktikan tiap perbaikan:

| Perbaikan | Dibuktikan oleh |
|---|---|
| Sintaks placeholder | `RbacKaryawanTest`, `RbacCustomerTest`, `RbacSuperadminTest`, `RbacRoleDatabaseTest` — keempatnya login memakai `app.security.password` |
| Kunci kripto | `CryptoConverterTest` dan `KaryawanControllerTest` — keduanya menyentuh `nik`/`npwp` |
| Kunci JWT | Seluruh test RBAC — semua menerbitkan dan memverifikasi token |

## Batasan Explicit

- Dokumen ini tidak memperbaiki `RedisConfigOld.java` — yang ternyata **bukan**
  kode mati: ia `@Configuration` aktif tanpa guard apa pun dan mendefinisikan
  bean `cacheManager`. Dokumen ini juga tidak menyentuh
  `spring.autoconfigure.exclude` di baris terakhir `application.properties`.
  Keduanya di luar lingkup CI/CD.
- **Duplikasi `spring.data.redis.*` dan `app.redis.*` dibiarkan.** Keduanya
  mendeskripsikan koneksi Redis yang sama dengan nama properti berbeda, dan
  hanya `app.redis.*` yang benar-benar menentukan koneksi. Keduanya kini
  membaca env yang sama (`REDIS_HOST` dan kawan-kawan) sehingga tidak bisa lagi
  menyimpang nilainya, tapi menghapus salah satunya adalah pembersihan kode di
  luar lingkup CI/CD.
- `app.security.cors-allowed-origins` masih hardcode di `application.properties`.
  Pemindahannya ke env ditangani di
  [impl-plan-application-properties-profiles.md](./impl-plan-application-properties-profiles.md),
  bukan di sini.
