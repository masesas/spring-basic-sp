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
