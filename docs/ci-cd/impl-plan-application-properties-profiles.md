# Impl Plan — Profile `application.properties`

Memisahkan konfigurasi menjadi satu berkas umum dan tiga berkas profile.

```
src/main/resources/
├── application.properties        identitas & kebijakan aplikasi      (21 properti)
├── application-local.properties  ./mvnw test + spring-boot:run       (34 properti)
├── application-dev.properties    container demo1-dev                 (34 properti)
└── application-prod.properties   container demo1-prod                (34 properti)
```

---

## Garis Pemisahnya

**Berkas umum hanya memuat yang tidak bergantung lingkungan** — identitas
aplikasi dan kebijakannya. **Berkas profile memuat alamat, kredensial, ukuran
sumber daya, dan tingkat log** — segala hal yang menggambarkan *di mana* dan
*sebesar apa* aplikasi berjalan.

| Berkas umum | Berkas profile |
|---|---|
| `spring.application.name`, `spring.config.import` | `server.port` |
| `server.shutdown`, `spring.lifecycle.timeout-per-shutdown-phase` | `spring.datasource.url` / `username` / `password` / `hikari.schema` / `hikari.maximum-pool-size` |
| `spring.datasource.driver-class-name` | `spring.jpa.properties.hibernate.default_schema` |
| `spring.jpa.hibernate.ddl-auto`, `show-sql`, `format_sql` | `spring.data.redis.*` (7 properti) |
| `spring.web.error.include-*` (4 properti) | `app.redis.*` (10 properti) |
| `app.security.jwt-ttl-minutes` | `app.security.jwt-secret` / `password` / `crypto-key` / `cors-allowed-origins` |
| `app.image.max-size`, `spring.servlet.multipart.*` | `app.dummy-json.*` (3 properti) |
| `logging.pattern.level` | `app.image.base-dir` |
| `spring.data.web.pageable.*`, `spring.autoconfigure.exclude` | `logging.level.*` |

**Tidak ada satu pun kunci yang muncul di dua berkas.** Berkas umum tidak
memegang nilai apa pun yang kelak dibayangi profile — kalau prod perlu nilai
berbeda, ia ditulis di berkas prod, bukan menimpa sesuatu dari berkas umum.

Konsekuensi yang harus dipatuhi: **tidak boleh ada skenario yang berjalan tanpa
profile.** Skenario tanpa profile tidak punya sumber konfigurasi database sama
sekali. Karena itu `local` ada dan diaktifkan dari `pom.xml`.

---

## Cara Profile Diaktifkan

| Skenario | Profile | Diaktifkan oleh | Sumber nilai |
|---|---|---|---|
| `./mvnw test` (lokal & CI) | `local` | `maven-surefire-plugin` di `pom.xml` | `.env` lokal / blok `env:` di `ci.yml` |
| `./mvnw spring-boot:run` | `local` | `spring-boot-maven-plugin` di `pom.xml` | `.env` lokal |
| Container dev | `dev` | `SPRING_PROFILES_ACTIVE` di `.env.app` | `.env.app` di server |
| Container prod | `prod` | `SPRING_PROFILES_ACTIVE` di `.env.app` | `.env.app` di server |

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <profiles>
            <profile>local</profile>
        </profiles>
    </configuration>
</plugin>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <systemPropertyVariables>
            <spring.profiles.active>local</spring.profiles.active>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

Diaktifkan di `pom.xml`, **bukan** lewat `@ActiveProfiles` di tiap kelas test —
satu tempat, berlaku untuk 17 kelas test tanpa satu pun disentuh.

**Nama variabel sama di keempat skenario.** Suffix `_DEV`/`_PROD` hidup di
GitHub dan dilucuti `scripts/render-env.sh` saat menyusun `.env.app` — lihat
[impl-plan-env-github.md](./impl-plan-env-github.md). Yang membedakan dev dari
prod adalah nilainya, bukan namanya.

---

## Perbedaan Isi Antar Profile

Ketiga berkas profile meng-override kunci yang persis sama. Yang berbeda hari
ini hanya empat hal:

| Properti | local | dev | prod |
|---|---|---|---|
| `spring.datasource.hikari.maximum-pool-size` | 5 | 5 | 10 |
| `logging.level.com.masesas.exercises.demo1` | `DEBUG` | `DEBUG` | `INFO` |
| `logging.level.org.springframework*` | `.security=INFO` | `.security=INFO` | `=WARN` |
| Nilai default pada placeholder non-kredensial | ada | tidak | tidak |

Selebihnya identik hari ini — dan memang boleh identik. Gunanya berkas terpisah
bukan supaya berbeda sekarang, tapi supaya bisa berbeda nanti tanpa menyentuh
apa pun selain berkas yang bersangkutan.

**Kenapa `local` boleh punya default sementara `dev` dan `prod` tidak.** Tiga
placeholder non-kredensial di `local` (`APP_CORS_ALLOWED_ORIGINS`,
`APP_IMAGE_BASE_DIR`, `DUMMYJSON_BASE_URL`) punya nilai bawaan yang masuk akal
untuk mesin pengembang, supaya `.env` tetap pendek. Di container tidak ada
default sama sekali: variabel yang hilang harus menghentikan deploy, bukan
diam-diam diganti nilai bawaan.

---

## Fail-Fast: Kenapa Validasi Diperlukan

Placeholder wajib tanpa default **tidak cukup** untuk menjamin container gagal
start saat sebuah variabel hilang. Ini terbukti saat pengujian:

```
$ (REDIS_HOST dihapus dari .env.app)
demo1-uji-d   Up 12 seconds (healthy)      curl /api/rolemap -> 200
```

Sebabnya, properti `app.*` di-bind lewat `@ConfigurationProperties`
(`AppConfigProperties`), dan Binder Spring **mengabaikan placeholder yang tidak
bisa di-resolve** — nilainya menjadi string literal. Terbukti saat endpoint
ber-cache dipanggil:

```
Unable to connect to ${REDIS_HOST}/<unresolved>:6379
```

Karena string `${REDIS_HOST}` tidak kosong, `@NotBlank` saja tidak menangkapnya.
Yang menangkap adalah pola yang menolak nilai berbentuk placeholder:

```java
private static final String BUKAN_PLACEHOLDER = "^(?!\\$\\{).+";
```

`AppConfigProperties` diberi `@Validated`, setiap sub-objek diberi `@Valid`, dan
sembilan field String yang nilainya datang dari environment diberi `@NotBlank` +
`@Pattern`. `spring-boot-starter-validation` sudah ada di `pom.xml`.

Hasilnya:

```
APPLICATION FAILED TO START

Binding to target ...AppConfigProperties failed:

    Property: app.redis.host
    Value: "${REDIS_HOST}"
    Reason: masih berbentuk placeholder: environment variable yang dirujuk belum diisi
```

Yang **sudah** gagal keras tanpa validasi ini, dan tetap begitu:

| Jalur | Kenapa gagal |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_SCHEMA` | DataSource dan JPA menyambung saat start |
| `JWT_SECRET`, `CRYPTO_KEY`, `APP_CORS_ALLOWED_ORIGINS`, `APP_REDIS_KEY_PREFIX` | dibaca lewat `@Value`, yang melempar saat placeholder gagal resolve |
| `APP_SERVER_PORT`, `REDIS_PORT` | bertipe angka; string literal gagal dikonversi |

Yang **hanya** tertangkap oleh validasi: `REDIS_HOST`, `REDIS_USERNAME`,
`REDIS_PASSWORD`, `DEMO_PASSWORD`, `DUMMYJSON_BASE_URL`, `APP_IMAGE_BASE_DIR`.

---

## Checklist

- [ ] `application.properties` tidak memuat satu pun kunci database, Redis, kredensial, atau tingkat log
- [ ] Keempat berkas dibuat; tidak ada kunci yang muncul di berkas umum sekaligus di berkas profile
- [ ] Ketiga berkas profile meng-override kunci yang sama persis
- [ ] Tidak ada nilai default pada placeholder di `application-dev.properties` dan `application-prod.properties`
- [ ] `pom.xml` mengaktifkan profile `local` untuk surefire dan `spring-boot:run`
- [ ] Tidak ada `@ActiveProfiles` di kelas test mana pun
- [ ] `AppConfigProperties` diberi `@Validated`, `@Valid`, `@NotBlank`, dan `@Pattern` penolak placeholder
- [ ] `.env`, `.env.example`, dan `ci.yml` memuat `APP_SERVER_PORT` dan `APP_REDIS_KEY_PREFIX`
- [ ] Tidak ada satu pun properti bersuffix `_DEV`/`_PROD` di keempat berkas

## Verifikasi

Tidak ada kunci yang dibayangi:

```bash
cd src/main/resources
comm -12 <(grep -oE '^[a-z][a-z0-9.-]*' application.properties      | sort -u) \
         <(grep -oE '^[a-z][a-z0-9.-]*' application-dev.properties  | sort -u)
```

Harus kosong.

Ketiga berkas profile meng-override kunci yang sama:

```bash
diff <(grep -oE '^[a-z][a-z0-9.-]*' application-local.properties) \
     <(grep -oE '^[a-z][a-z0-9.-]*' application-dev.properties)     # harus kosong

diff <(grep -oE '^[a-z][a-z0-9.-]*' application-dev.properties) \
     <(grep -oE '^[a-z][a-z0-9.-]*' application-prod.properties)
```

Yang kedua hanya boleh menampilkan pasangan
`logging.level.org.springframework.security` (dev) dan
`logging.level.org.springframework` (prod).

Tidak ada suffix tersisa pada baris kode:

```bash
grep -vE '^\s*#' src/main/resources/application*.properties | grep -cE '_DEV|_PROD'   # harus 0
```

## Test Verifikasi

```bash
./mvnw clean test
```

Harus hijau dan log harus menyebut profile yang aktif:

```
The following 1 profile is active: "local"
Tests run: 158, Failures: 0, Errors: 0, Skipped: 0
```

Fail-fast dibuktikan dengan menghapus satu variabel dari `.env.app` lalu
menjalankan container — lihat tabel di bagian Fail-Fast. Container harus masuk
crash loop dengan `APPLICATION FAILED TO START`, bukan menjadi `healthy`.

## Batasan Explicit

- **Tiga berkas profile hampir identik hari ini.** Itu disengaja dan diterima:
  biayanya adalah properti baru harus ditambahkan ke tiga tempat, manfaatnya
  adalah setiap lingkungan bisa menyimpang tanpa menyentuh yang lain. Perintah
  `diff` di atas ada untuk menahan biayanya.
- **Tidak ada test yang menjalankan profile `dev` atau `prod`.** Isi kedua
  berkas itu dibuktikan saat container start, bukan saat test.
- **Validasi hanya menjaga properti di bawah prefix `app`.** Properti `spring.*`
  di-bind Spring Boot sendiri dan tidak bisa diberi anotasi dari sini.
  Praktiknya tidak masalah: `spring.datasource.*` gagal keras karena koneksi
  eager, dan `spring.data.redis.*` tidak menentukan koneksi apa pun karena
  `RedisConfig` membangun `RedisConnectionFactory` sendiri dari `app.redis.*`.
- **`spring.data.redis.*` dan `app.redis.*` masih duplikat.** Keduanya membaca
  environment variable yang sama sehingga tidak bisa menyimpang nilainya, tapi
  hanya yang kedua yang berpengaruh. Menghapus salah satunya pembersihan kode di
  luar lingkup CI/CD.
- Profile `dev` dan `prod` menunjuk database yang sama. `maximum-pool-size` 5
  dan 10 berarti sampai 15 koneksi dari satu server ke satu PostgreSQL, di luar
  koneksi dari mesin pengembang dan dari CI.
