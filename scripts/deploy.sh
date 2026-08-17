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

echo "==> Menarik image"
docker pull "$APP_IMAGE"

# Direktori volume disiapkan di sini, bukan manual di server. `mkdir` cukup
# dijalankan sebagai user deploy karena direktori induknya miliknya sendiri.
# `chown` ke UID 1001 — UID user di dalam container — butuh root, dan itu
# didapat lewat container sekali pakai alih-alih `sudo`, supaya user deploy
# tetap tanpa hak istimewa apa pun di server. Image yang dipakai adalah image
# aplikasi yang baru ditarik, jadi tidak ada dependensi baru.
echo "==> Menyiapkan direktori data $APP_DATA_DIR"
mkdir -p "$APP_DATA_DIR"
if [ "$(stat -c %u "$APP_DATA_DIR")" != "1001" ]; then
    docker run --rm -u 0 --entrypoint chown \
        -v "$APP_DATA_DIR:/data" "$APP_IMAGE" -R 1001:1001 /data
fi

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
