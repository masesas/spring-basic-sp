# Impl Plan — Script Deploy

Dua script bash di `./scripts/`. Keduanya ada supaya berkas workflow tetap pendek
dan bisa dibaca sebagai rangkaian langkah, bukan sebagai dinding bash.

| Script | Jalan di | Tugas |
|---|---|---|
| `scripts/render-env.sh` | runner GitHub | Menghasilkan `.env.deploy` dan `.env.app` dari environment job |
| `scripts/deploy.sh` | server Ubuntu | Menarik image, menjalankan compose, menunggu sehat, membersihkan |

Keduanya diberi izin eksekusi: `chmod +x scripts/*.sh`.

---

## `scripts/render-env.sh`

Dipanggil dengan suffix profile dan direktori tujuan:

```bash
scripts/render-env.sh DEV ./keluaran
```

Seluruh nilai dibacanya dari environment job — workflow yang memetakan
`secrets.*` dan `vars.*` ke `env:`. Script ini tidak pernah menyentuh GitHub API
dan tidak tahu apa-apa soal secret.

```bash
#!/usr/bin/env bash
#
# Menghasilkan .env.deploy dan .env.app untuk satu profile.
#
#   scripts/render-env.sh <DEV|PROD> <direktori-tujuan>
#
# Seluruh nilai dibaca dari environment. Variabel yang wajib ada didaftarkan
# di bawah dan diperiksa lebih dulu — lebih baik gagal di sini daripada gagal
# saat container sudah berjalan setengah jalan di server.

set -euo pipefail

SUFFIX="${1:?pemakaian: render-env.sh <DEV|PROD> <direktori-tujuan>}"
TUJUAN="${2:?pemakaian: render-env.sh <DEV|PROD> <direktori-tujuan>}"

if [ "$SUFFIX" != "DEV" ] && [ "$SUFFIX" != "PROD" ]; then
    echo "render-env: suffix harus DEV atau PROD, bukan '$SUFFIX'" >&2
    exit 1
fi

WAJIB_BERSUFFIX=(
    SPRING_PROFILES_ACTIVE APP_SERVER_PORT APP_HOST_PORT
    APP_REGISTRY APP_IMAGE_NAME APP_CONTAINER_NAME
    APP_DATA_DIR APP_IMAGE_BASE_DIR
    APP_CORS_ALLOWED_ORIGINS APP_REDIS_KEY_PREFIX
    APP_TZ APP_JAVA_TOOL_OPTIONS
    DB_URL DB_USERNAME DB_PASSWORD DB_SCHEMA
    REDIS_HOST REDIS_PORT REDIS_USERNAME REDIS_PASSWORD
    JWT_SECRET CRYPTO_KEY DEMO_PASSWORD
    DUMMYJSON_BASE_URL
)

kurang=0
for nama in "${WAJIB_BERSUFFIX[@]}"; do
    penuh="${nama}_${SUFFIX}"
    if [ -z "${!penuh:-}" ]; then
        echo "render-env: $penuh kosong atau belum diisi di GitHub" >&2
        kurang=1
    fi
done
[ -z "${APP_IMAGE_TAG:-}" ] && { echo "render-env: APP_IMAGE_TAG kosong" >&2; kurang=1; }
[ "$kurang" -eq 0 ] || exit 1

nilai() { local n="${1}_${SUFFIX}"; printf '%s' "${!n}"; }

umask 077
mkdir -p "$TUJUAN"

# --- .env.deploy: hanya untuk interpolasi docker-compose.yml, tanpa suffix ---
cat > "$TUJUAN/.env.deploy" <<EOF
COMPOSE_PROJECT_NAME=$(nilai APP_CONTAINER_NAME)
APP_IMAGE=$(nilai APP_REGISTRY)/$(nilai APP_IMAGE_NAME):${APP_IMAGE_TAG}
APP_CONTAINER_NAME=$(nilai APP_CONTAINER_NAME)
APP_HOST_PORT=$(nilai APP_HOST_PORT)
APP_CONTAINER_PORT=$(nilai APP_SERVER_PORT)
APP_DATA_DIR=$(nilai APP_DATA_DIR)
APP_IMAGE_BASE_DIR=$(nilai APP_IMAGE_BASE_DIR)
EOF

# --- .env.app: environment proses Java di dalam container ---
#
# Suffix _DEV/_PROD dilucuti di sini. Yang membedakan dev dan prod adalah nilai
# yang dipilih lewat $SUFFIX, bukan nama yang dibaca aplikasi — sehingga
# application.properties memakai satu nama untuk container, test, dan lokal.
#
# Tiga baris pertama tidak sekadar dilucuti tapi berganti nama, karena namanya
# ditentukan Spring Boot dan JVM: APP_TZ menjadi TZ, bukan APP_TZ.
{
    echo "SPRING_PROFILES_ACTIVE=$(nilai SPRING_PROFILES_ACTIVE)"
    echo "TZ=$(nilai APP_TZ)"
    echo "JAVA_TOOL_OPTIONS=$(nilai APP_JAVA_TOOL_OPTIONS)"
    echo
    env \
      | grep -E "^[A-Z0-9_]+_${SUFFIX}=" \
      | grep -vE "^(SSH_|GHCR_|SPRING_PROFILES_ACTIVE_|APP_TZ_|APP_JAVA_TOOL_OPTIONS_|APP_REGISTRY_|APP_IMAGE_NAME_|APP_CONTAINER_NAME_|APP_HOST_PORT_|APP_DEPLOY_DIR_|APP_DATA_DIR_)" \
      | sed -E "s/^([A-Z0-9_]+)_${SUFFIX}=/\1=/" \
      | sort
} > "$TUJUAN/.env.app"

echo "render-env: $TUJUAN/.env.deploy dan $TUJUAN/.env.app siap"
```

Yang dikecualikan dari `.env.app` adalah variabel yang tidak ada urusannya dengan
proses Java: kredensial SSH dan GHCR, serta variabel yang hanya dipakai Compose.
`APP_IMAGE_BASE_DIR_<SUFFIX>` **tidak** dikecualikan — aplikasi memang
membutuhkannya untuk `app.image.base-dir`.

Urutan `grep -v` lalu `sed` penting: penyaringan bekerja pada nama bersuffix,
jadi pola pengecualian di atas ditulis dengan garis bawah di ujung
(`APP_DATA_DIR_`) dan tetap cocok. Kalau `sed` dijalankan lebih dulu,
`APP_DATA_DIR` yang sudah terlucuti tidak lagi tertangkap pola itu dan variabel
khusus Compose ikut bocor ke dalam container.

Perhatikan bahwa `SSH_*` dan `GHCR_*` disaring dua lapis: lewat pola pengecualian
di atas, dan dengan tidak memasukkannya ke `env:` job render sama sekali di
workflow. Lapis kedua yang sebenarnya menjaga; yang pertama jaring pengaman.

---

## `scripts/deploy.sh`

Dikirim ke server bersama `docker-compose.yml`, `.env.deploy`, dan `.env.app`,
lalu dijalankan lewat SSH. Script ini tidak menerima argumen — seluruh
keputusan sudah tertulis di `.env.deploy` yang menyertainya.

```bash
#!/usr/bin/env bash
#
# Dijalankan DI SERVER, di dalam direktori deploy yang memuat:
#   docker-compose.yml  .env.deploy  .env.app  deploy.sh
#
# Tidak ada source code di server. Yang ditarik hanyalah image jadi dari GHCR.

set -euo pipefail

cd "$(dirname "$0")"

for berkas in docker-compose.yml .env.deploy .env.app; do
    [ -f "$berkas" ] || { echo "deploy: $berkas tidak ditemukan"; exit 1; }
done

set -a
. ./.env.deploy
set +a

echo "==> Image  : $APP_IMAGE"
echo "==> Container: $APP_CONTAINER_NAME (port host $APP_HOST_PORT)"

# Direktori volume dibuat sekali secara manual dengan pemilik UID 1001 —
# lihat impl-plan-prasyarat-server.md. Di sini hanya diperiksa.
if [ ! -d "$APP_DATA_DIR" ]; then
    echo "deploy: direktori data $APP_DATA_DIR belum ada." >&2
    echo "        Buat dulu: sudo mkdir -p $APP_DATA_DIR && sudo chown -R 1001:1001 $APP_DATA_DIR" >&2
    exit 1
fi

echo "==> Menarik image"
docker pull "$APP_IMAGE"

echo "==> Menjalankan container"
docker compose --env-file .env.deploy up -d --remove-orphans

echo "==> Menunggu status healthy"
sehat=0
for _ in $(seq 1 36); do
    status="$(docker inspect --format '{{.State.Health.Status}}' "$APP_CONTAINER_NAME" 2>/dev/null || echo belum)"
    if [ "$status" = "healthy" ]; then sehat=1; break; fi
    if [ "$status" = "unhealthy" ]; then break; fi
    sleep 5
done

if [ "$sehat" -ne 1 ]; then
    echo "deploy: container tidak menjadi healthy dalam 3 menit (status terakhir: ${status:-tidak diketahui})" >&2
    echo "--- 80 baris log terakhir ---" >&2
    docker logs --tail 80 "$APP_CONTAINER_NAME" >&2 || true
    exit 1
fi

echo "==> Sehat. Membersihkan image tanpa tag"
# Sengaja bukan `prune -a`: satu daemon Docker dipakai bersama oleh container
# dev dan prod, dan `-a` akan menghapus image profile lain yang sedang tidak
# berjalan tapi masih dibutuhkan untuk rollback.
docker image prune -f

echo "==> Selesai: $APP_IMAGE"
```

---

## Rollback

Tidak ada mekanisme otomatis. Yang disediakan adalah jalannya: setiap deploy juga
menerbitkan tag `<ddMMyyyy>-<sha7>` yang tidak pernah ditimpa. Untuk kembali ke
versi sebelumnya, di server:

```bash
cd /home/masesas/apps/demo1-prod
sed -i 's|^APP_IMAGE=.*|APP_IMAGE=ghcr.io/masesas/demo1-prod:16082026-9f3c1ab|' .env.deploy
./deploy.sh
```

Deploy berikutnya dari GitHub akan menimpa `.env.deploy` dan mengembalikan
aplikasi ke versi terbaru — rollback ini bertahan sampai push berikutnya, bukan
selamanya.

---

## Checklist

- [ ] `scripts/render-env.sh` dibuat dan `chmod +x`
- [ ] `scripts/deploy.sh` dibuat dan `chmod +x`
- [ ] Keduanya memakai `set -euo pipefail`
- [ ] `render-env.sh` memeriksa seluruh variabel wajib sebelum menulis apa pun
- [ ] `render-env.sh` memakai `umask 077` sebelum menulis berkas berisi kredensial
- [ ] `.env.app` yang dihasilkan tidak memuat satu pun `SSH_` atau `GHCR_`
- [ ] `.env.app` yang dihasilkan tidak memuat satu pun sisa suffix `_DEV`/`_PROD`
- [ ] `.env.app` tidak memuat variabel khusus Compose (`APP_REGISTRY`, `APP_IMAGE_NAME`, `APP_CONTAINER_NAME`, `APP_HOST_PORT`, `APP_DEPLOY_DIR`, `APP_DATA_DIR`)
- [ ] `deploy.sh` gagal dengan exit code bukan nol saat container tidak sehat
- [ ] `deploy.sh` mencetak log container saat gagal
- [ ] `deploy.sh` tidak pernah memakai `docker system prune -a`

## Verifikasi

Uji `render-env.sh` secara lokal tanpa menyentuh server:

```bash
set -a; . ./.env.server.dev; set +a
APP_IMAGE_TAG="$(TZ=Asia/Jakarta date +%d%m%Y)-latest" \
  scripts/render-env.sh DEV /tmp/uji-render

cat /tmp/uji-render/.env.deploy
grep -c "_DEV" /tmp/uji-render/.env.app           # harus 0 — suffix sudah dilucuti
grep -cE "^(SSH_|GHCR_)" /tmp/uji-render/.env.app # harus 0
grep -cE "^(APP_REGISTRY|APP_IMAGE_NAME|APP_CONTAINER_NAME|APP_HOST_PORT|APP_DEPLOY_DIR|APP_DATA_DIR)=" \
     /tmp/uji-render/.env.app                     # harus 0 — khusus Compose
grep -c "^SPRING_PROFILES_ACTIVE=" /tmp/uji-render/.env.app  # harus 1
grep -c "^DB_URL=" /tmp/uji-render/.env.app       # harus 1 — nama yang dibaca aplikasi
stat -f '%Lp' /tmp/uji-render/.env.app            # harus 600 (Linux: stat -c '%a')
rm -rf /tmp/uji-render
```

Uji variabel yang kurang harus ditolak:

```bash
env -u JWT_SECRET_DEV bash -c '
  set -a; . ./.env.server.dev; set +a
  unset JWT_SECRET_DEV
  APP_IMAGE_TAG=uji scripts/render-env.sh DEV /tmp/uji-gagal
'   # harus keluar dengan pesan "JWT_SECRET_DEV kosong" dan exit code 1
```

## Batasan Explicit

- **Nilai bermultibaris tidak didukung `.env.app`.** Penyaringan memakai `env |
  grep` yang berbasis baris. Tidak ada variabel aplikasi yang bermultibaris saat
  ini; `SSH_PRIVATE_KEY_*` yang memang multibaris tidak pernah masuk ke script ini.
- **`deploy.sh` tidak memverifikasi bahwa image yang ditarik memang hasil build
  commit ini.** Kalau dua deploy berjalan hampir bersamaan, tag `-latest` bisa
  sudah berganti di antara push dan pull. `concurrency` di workflow yang
  mencegahnya, bukan script ini.
- **Tidak ada rollback otomatis saat health check gagal.** Container baru
  ditinggalkan dalam keadaan tidak sehat dan workflow ditandai merah. Ini disengaja:
  rollback otomatis yang salah menebak lebih berbahaya daripada kegagalan yang terlihat.
- **`docker image prune -f` tetap menghapus image lama tanpa tag.** Rollback hanya
  mungkin ke tag `<ddMMyyyy>-<sha7>` yang masih ada di GHCR, bukan ke image yang
  tersisa di server.
