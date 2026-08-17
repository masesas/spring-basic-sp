# Impl Plan — Dockerfile

Satu `Dockerfile` multi-stage untuk kedua profile, plus `.dockerignore`.

---

## Keputusan: Satu Dockerfile, Bukan Satu per Profile

Pertanyaannya wajar, dan jawabannya tidak: **artefak build dev dan prod harus
identik.**

Profile adalah keputusan *runtime*, bukan *build-time*. Yang membedakan dev dan
prod cuma `SPRING_PROFILES_ACTIVE`, nilai environment variable, dan port yang
dipublikasikan — tidak satu pun dari itu mengubah isi jar. Dua Dockerfile berarti
dua image yang bisa diam-diam menyimpang: satu naik versi base image, satu
tertinggal; satu dapat perbaikan, satu terlewat. Dan begitu itu terjadi,
jaminan paling berharga dari sebuah pipeline hilang — bahwa biner yang lulus test
di dev adalah biner yang sama persis yang jalan di prod.

Yang tetap dipisah adalah **nama image**-nya (`demo1-dev` vs `demo1-prod`), sesuai
permintaan penamaan. Keduanya dibangun dari `Dockerfile` yang sama.

Pertimbangan yang sama berlaku untuk `docker-compose.yml` dan dibahas di
[impl-plan-docker-compose.md](./impl-plan-docker-compose.md).

---

## `docker/Dockerfile`

```dockerfile
# ============================================================================
#  Stage 1 — build jar
#
#  pom.xml disalin duluan dan dependency diunduh di layer terpisah, supaya
#  perubahan pada src/ tidak memaksa seluruh dependency diunduh ulang.
# ============================================================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

# ============================================================================
#  Stage 2 — runtime
#
#  JRE saja, tanpa Maven dan tanpa source code. Berjalan sebagai user tanpa
#  hak istimewa dengan UID tetap 1001 — UID-nya harus tetap karena direktori
#  volume di host dimiliki UID yang sama.
# ============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S -g 1001 app \
 && adduser  -S -u 1001 -G app app

WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

RUN mkdir -p /app/resources/images \
 && chown -R app:app /app

USER app

EXPOSE 8080

# Endpoint /api/rolemap ada di whitelist SecurityConfig, jadi membalas 200 tanpa
# token. Balasan 200 membuktikan JVM hidup, port terbuka, dan konteks Spring
# selesai dimuat — aplikasi yang gagal me-resolve satu placeholder saja tidak
# akan pernah sampai ke titik ini.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/api/rolemap || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Catatan atas beberapa pilihan:

- **`maven:3.9-eclipse-temurin-21`**, bukan `./mvnw`. Wrapper repositori ini
  bertipe `only-script` dan akan mengunduh distribusi Maven setiap build image.
  Base image Maven sudah membawanya.
- **`-DskipTests` di kedua perintah.** Test sudah dijalankan lengkap oleh CI
  sebelum image dibangun — lihat
  [impl-plan-workflow-ci.md](./impl-plan-workflow-ci.md). Menjalankannya lagi di
  dalam Docker berarti build image butuh akses ke PostgreSQL dan Redis, dan itu
  menggagalkan seluruh gagasan image yang bisa dibangun di mana saja.
- **`target/*.jar`.** `spring-boot-maven-plugin` menghasilkan tepat satu berkas
  `.jar` (yang asli sebelum repackage disimpan sebagai `.jar.original` dan tidak
  cocok dengan pola ini).
- **UID 1001 ditulis eksplisit.** Volume host di-mount ke `/app/resources/images`;
  kalau UID container tidak tetap, unggahan avatar akan gagal dengan
  `Permission denied` setelah base image berganti versi.
- **`--start-period=90s`.** Selama periode ini kegagalan health check tidak
  dihitung. Aplikasi Spring Boot dengan koneksi PostgreSQL dan Redis remote butuh
  waktu lebih lama dari default 0 detik untuk siap.
- **`spring-boot-devtools`** ikut di `pom.xml` tapi bertanda `optional` dan
  otomatis nonaktif saat aplikasi berjalan dari jar. Tidak perlu dikecualikan
  secara khusus.

---

## `.dockerignore`

Diletakkan di **root repositori**, bukan di `docker/`. Docker membaca berkas ini
relatif terhadap build context, dan build context-nya adalah root.

```
# Hasil build lokal — akan dibangun ulang di dalam image
target/

# Kredensial. Yang paling penting di berkas ini.
.env
.env.*
!.env.example

# Riwayat dan metadata
.git
.gitignore
.gitattributes
.github
.mvn
mvnw
mvnw.cmd

# Tidak dibutuhkan runtime
docs/
http/
scripts/
docker/
*.md
*.sql
dependency-check-suppressions.xml
.claude/
.remember/
.idea/
.vscode/
.DS_Store
```

Tanpa berkas ini, `.env` lokal berisi kredensial nyata akan ikut terkirim ke
Docker daemon dan berpeluang masuk ke dalam layer image.

---

## Build & Uji Lokal

```bash
# Dibangun dari root repositori — build context adalah root
docker build -f docker/Dockerfile -t demo1-dev:lokal .

# Jalankan dengan env minimal untuk membuktikan image-nya benar
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e APP_SERVER_PORT_DEV=8080 \
  -e DB_URL_DEV="jdbc:postgresql://129.226.195.9:5432/binar_finance" \
  -e DB_USERNAME_DEV=binar_admin \
  -e DB_PASSWORD_DEV=binar_bc_password \
  -e DB_SCHEMA_DEV=masesas \
  -e REDIS_HOST_DEV=129.226.195.9 \
  -e REDIS_PORT_DEV=6379 \
  -e REDIS_USERNAME_DEV=binar_app \
  -e REDIS_PASSWORD_DEV=binar_bc_password \
  -e JWT_SECRET_DEV="$(openssl rand -base64 48)" \
  -e CRYPTO_KEY_DEV="np1iEu6vzSPjj+5KveQmFWE+zQ/bM3DuNJKDCkVKYZY=" \
  -e DEMO_PASSWORD_DEV=password123 \
  -e APP_CORS_ALLOWED_ORIGINS_DEV="http://localhost:3000" \
  -e APP_REDIS_KEY_PREFIX_DEV="demo:lokal" \
  -e APP_IMAGE_BASE_DIR_DEV=/app/resources/images \
  -e DUMMYJSON_BASE_URL_DEV=https://dummyjson.com \
  demo1-dev:lokal
```

Prefix Redis sengaja `demo:lokal`, bukan `demo:dev`, supaya uji coba tidak
menimpa cache container dev yang sedang jalan di server.

## Checklist

- [ ] `docker/Dockerfile` dibuat dengan dua stage
- [ ] `.dockerignore` dibuat di root repositori dan memuat `.env.*`
- [ ] Image berhasil dibangun: `docker build -f docker/Dockerfile -t demo1-dev:lokal .`
- [ ] Container berjalan dan `curl -s -o /dev/null -w '%{http_code}' localhost:8080/api/rolemap` mengembalikan `200`
- [ ] `docker inspect --format='{{.State.Health.Status}}' <container>` menjadi `healthy` dalam 2 menit
- [ ] `docker run --rm demo1-dev:lokal id` menampilkan `uid=1001`
- [ ] `docker history demo1-dev:lokal` tidak menampilkan layer yang menyalin `.env`

## Batasan Explicit

- **Image tidak reproducible byte-per-byte.** Tag `maven:3.9-eclipse-temurin-21`
  dan `eclipse-temurin:21-jre-alpine` bergerak. Menyematkan digest membuatnya
  reproducible tapi menahan perbaikan keamanan base image sampai ada yang ingat
  memperbaruinya. Untuk latihan ini, tag yang bergerak dipilih dengan sadar.
- **Health check hanya membuktikan aplikasi hidup, bukan siap melayani seluruh
  fitur.** `/api/rolemap` tidak menyentuh Redis. Container bisa berstatus
  `healthy` sementara Redis tidak terjangkau.
- **Tidak ada `spring-boot-starter-actuator`.** Kalau nanti ditambahkan,
  `/actuator/health` adalah target health check yang lebih tepat karena ikut
  memeriksa DataSource dan Redis. Konsekuensinya endpoint itu perlu dimasukkan ke
  whitelist `SecurityConfig` atau dipindah ke management port terpisah — dua-duanya
  di luar lingkup dokumen ini.
- **Layer cache dependency bergantung pada `pom.xml` tidak berubah.** Setiap
  perubahan `pom.xml` sekecil apa pun memicu unduh ulang seluruh dependency di
  build berikutnya.
