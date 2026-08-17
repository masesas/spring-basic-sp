# Impl Plan — docker-compose

Satu `docker/docker-compose.yml` yang dipakai kedua profile, diparameterisasi
lewat berkas env.

---

## Keputusan: Satu Compose File, Diparameterisasi

Yang berbeda antara dev dan prod hanya empat hal: nama image, nama container,
port host, dan direktori data. Keempatnya cocok jadi variabel. Dua berkas compose
yang 90% identik hanya menambah tempat untuk lupa menyalin perubahan — dan
`CLAUDE.md` sudah menyatakan preferensinya: kalau ragu antara dua rancangan,
pilih yang lebih sedikit berkasnya.

Compose dijalankan begini di server:

```bash
docker compose --env-file .env.deploy up -d
```

## Dua Berkas Env, Bukan Satu

Ini bagian yang paling mudah salah dipahami, jadi ditulis eksplisit:

| Berkas | Dibaca oleh | Isi | Suffix |
|---|---|---|---|
| `.env.deploy` | Docker Compose, untuk mengisi `${...}` di dalam YAML | nama image, nama container, port, direktori | tanpa suffix |
| `.env.app` | Container, sebagai environment variable proses Java | kredensial & konfigurasi aplikasi | tanpa suffix — dilucuti `render-env.sh` |

`--env-file` hanya memengaruhi interpolasi YAML dan **tidak** menyuntikkan apa pun
ke dalam container. Sebaliknya `env_file:` di dalam YAML hanya menyuntikkan ke
container dan tidak bisa dipakai untuk interpolasi. Karena itu keduanya harus
berdiri sendiri.

Suffix `_DEV`/`_PROD` tidak masuk ke salah satu pun dari kedua berkas. Ia hidup
di GitHub — sebagai pembeda nilai dev dan prod — lalu dilucuti `render-env.sh`
saat berkas ini disusun. Baik Compose maupun proses Java selalu melihat nama yang
sama apa pun profile-nya.

Kedua berkas dihasilkan `scripts/render-env.sh` di runner lalu dikirim ke server —
lihat [impl-plan-scripts-deploy.md](./impl-plan-scripts-deploy.md).

---

## `docker/docker-compose.yml`

```yaml
# ============================================================================
#  Compose untuk satu service saja: aplikasi.
#
#  PostgreSQL dan Redis sengaja TIDAK ada di sini. Keduanya layanan eksternal
#  yang sudah berjalan dan dipakai bersama oleh dev, prod, dan mesin pengembang.
#  Menambahkannya di sini akan membuat compose down ikut menghapus data.
#
#  Dijalankan oleh scripts/deploy.sh:
#      docker compose --env-file .env.deploy up -d
# ============================================================================

services:
  app:
    image: ${APP_IMAGE}
    container_name: ${APP_CONTAINER_NAME}
    restart: unless-stopped

    env_file:
      - .env.app

    ports:
      - "${APP_HOST_PORT}:${APP_CONTAINER_PORT}"

    volumes:
      # Berkas unggahan (avatar karyawan) hidup di host, bukan di dalam
      # container, supaya tidak ikut hilang saat image diganti.
      - ${APP_DATA_DIR}:${APP_IMAGE_BASE_DIR}

    # Lebih lama dari spring.lifecycle.timeout-per-shutdown-phase (20s), supaya
    # JVM sempat menyelesaikan graceful shutdown sebelum menerima SIGKILL.
    stop_grace_period: 30s

    mem_limit: 768m

    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "5"
```

Health check tidak ditulis di sini karena sudah menjadi bagian dari image
(`HEALTHCHECK` di Dockerfile). Menulisnya dua kali membuka peluang keduanya
berbeda.

`mem_limit: 768m` berpasangan dengan `-XX:MaxRAMPercentage=75` di
`APP_JAVA_TOOL_OPTIONS_*`: heap maksimum menjadi sekitar 576 MB, sisanya untuk
metaspace, thread stack, dan buffer di luar heap.

---

## Contoh Isi Berkas Env di Server

`.env.deploy` untuk dev, dihasilkan otomatis:

```properties
COMPOSE_PROJECT_NAME=demo1-dev
APP_IMAGE=ghcr.io/masesas/demo1-dev:17082026-latest
APP_CONTAINER_NAME=demo1-dev
APP_HOST_PORT=8080
APP_CONTAINER_PORT=8080
APP_DATA_DIR=/home/masesas/data/demo1-dev/images
APP_IMAGE_BASE_DIR=/app/resources/images
```

`.env.app` untuk dev, dihasilkan otomatis:

```properties
SPRING_PROFILES_ACTIVE=dev
TZ=Asia/Jakarta
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75

APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
APP_IMAGE_BASE_DIR=/app/resources/images
APP_REDIS_KEY_PREFIX=demo:dev
APP_SERVER_PORT=8080
CRYPTO_KEY=...
DB_PASSWORD=...
DB_SCHEMA=masesas
DB_URL=jdbc:postgresql://129.226.195.9:5432/binar_finance
DB_USERNAME=binar_admin
DEMO_PASSWORD=password123
DUMMYJSON_BASE_URL=https://dummyjson.com
JWT_SECRET=...
REDIS_HOST=129.226.195.9
REDIS_PASSWORD=...
REDIS_PORT=6379
REDIS_USERNAME=binar_app
```

Tidak ada satu pun nama bersuffix di sini. Yang membedakan dev dari prod adalah
**nilainya**, yang dipilih `render-env.sh` dari variabel bersuffix di
environment job — nama yang sampai ke proses Java selalu sama. Karena itu
`application.properties` bisa melayani container, test, dan pengembangan lokal
dengan satu set placeholder. Alasan lengkapnya ada di
[impl-plan-env-github.md](./impl-plan-env-github.md).

Tiga baris pertama tidak sekadar dilucuti tapi berganti nama: `APP_TZ_<S>`
menjadi `TZ`, `APP_JAVA_TOOL_OPTIONS_<S>` menjadi `JAVA_TOOL_OPTIONS`. Nama
tujuannya ditentukan Spring Boot dan JVM, bukan oleh kita.

`APP_IMAGE_BASE_DIR` muncul di kedua berkas — di `.env.deploy` sebagai target
mount, di `.env.app` sebagai nilai `app.image.base-dir`. Keduanya **wajib
bernilai sama**; kalau berbeda, aplikasi menulis ke direktori yang tidak
di-mount dan unggahan hilang tanpa satu pun pesan error.

---

## Uji Lokal

```bash
mkdir -p /tmp/demo1-uji/images

cat > /tmp/demo1-uji/.env.deploy <<'EOF'
COMPOSE_PROJECT_NAME=demo1-uji
APP_IMAGE=demo1-dev:lokal
APP_CONTAINER_NAME=demo1-uji
APP_HOST_PORT=8080
APP_CONTAINER_PORT=8080
APP_DATA_DIR=/tmp/demo1-uji/images
APP_IMAGE_BASE_DIR=/app/resources/images
EOF

# .env.app diisi manual mengikuti contoh di atas
cp docker/docker-compose.yml /tmp/demo1-uji/
cd /tmp/demo1-uji && docker compose --env-file .env.deploy up -d
docker compose --env-file .env.deploy ps
```

## Checklist

- [ ] `docker/docker-compose.yml` dibuat, hanya berisi satu service `app`
- [ ] Tidak ada service PostgreSQL maupun Redis di dalamnya
- [ ] Tidak ada satu pun nama bersuffix `_DEV`/`_PROD` di dalam YAML
- [ ] Tidak ada nilai kredensial tertulis di dalam YAML
- [ ] `stop_grace_period` lebih besar dari `spring.lifecycle.timeout-per-shutdown-phase`
- [ ] `APP_IMAGE_BASE_DIR` di `.env.deploy` sama dengan `APP_IMAGE_BASE_DIR_<SUFFIX>` di `.env.app`
- [ ] `docker compose --env-file .env.deploy config` mencetak YAML tanpa variabel yang kosong

## Verifikasi

```bash
# Seluruh ${...} harus sudah terisi; tidak boleh ada nilai kosong
docker compose --env-file .env.deploy config

# Container dev dan prod harus memakai port host berbeda
docker ps --format '{{.Names}}\t{{.Ports}}'
```

## Batasan Explicit

- **`docker compose up -d` menghentikan container lama sebelum yang baru siap.**
  Ada jeda beberapa detik tanpa layanan setiap deploy. Zero-downtime butuh
  reverse proxy dan dua container berjalan bersamaan — di luar lingkup.
- **`mem_limit` mematikan container yang melampauinya (OOM kill), bukan
  melambatkannya.** Kalau container dev sering mati sendiri, angka 768m yang
  perlu dinaikkan, bukan restart policy yang perlu diubah.
- **Volume unggahan tidak ikut di-backup oleh apa pun.** Direktori
  `APP_DATA_DIR` hidup di host dan tidak masuk pipeline mana pun.
- **Dev dan prod berbagi satu daemon Docker di satu server.** `docker system
  prune` yang tidak hati-hati bisa menghapus image yang sedang dipakai profile
  lain. `deploy.sh` karena itu hanya membersihkan image tanpa tag, tidak pernah
  memakai `prune -a`.
