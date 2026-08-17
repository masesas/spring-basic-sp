# Impl Plan — Workflow CD Development

`.github/workflows/deploy-dev.yml` — terpicu oleh push ke `main` dan bisa
dijalankan manual. Tiga job berurutan: test, build & push image, deploy.

---

## Bentuk Umum

```
push ke main / Run workflow
        │
        ▼
  job ci ─── uses: ./.github/workflows/ci.yml
        │    merah ⇒ dua job berikutnya tidak pernah jalan
        ▼
  job build-push ─── docker build → push 2 tag ke ghcr.io/masesas/demo1-dev
        │
        ▼
  job deploy ─── render env → scp 4 berkas → ssh deploy.sh → curl verifikasi
```

Job dipisah tiga, bukan satu dengan banyak step, supaya kegagalan langsung
terbaca dari namanya di daftar Actions: gagal di test, gagal saat membangun
image, atau gagal di server.

---

## `.github/workflows/deploy-dev.yml`

```yaml
name: Deploy Development

on:
  push:
    branches: [main]
  workflow_dispatch:

# Dua deploy yang berjalan bersamaan akan saling menimpa tag <ddMMyyyy>-latest
# dan bisa membuat server menarik image yang bukan dari commit ini.
concurrency:
  group: deploy-dev
  cancel-in-progress: false

permissions:
  contents: read
  packages: write

jobs:
  ci:
    name: Test
    uses: ./.github/workflows/ci.yml
    secrets: inherit

  build-push:
    name: Build & push image
    needs: ci
    runs-on: ubuntu-latest
    environment: development
    timeout-minutes: 20
    outputs:
      tag: ${{ steps.tag.outputs.tag }}
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Susun tag image
        id: tag
        run: |
          tanggal="$(TZ=Asia/Jakarta date +%d%m%Y)"
          echo "tag=${tanggal}-latest"                  >> "$GITHUB_OUTPUT"
          echo "tag_sha=${tanggal}-${GITHUB_SHA:0:7}"   >> "$GITHUB_OUTPUT"

      - name: Siapkan Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login ke GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build & push
        uses: docker/build-push-action@v6
        with:
          context: .
          file: docker/Dockerfile
          push: true
          tags: |
            ${{ vars.APP_REGISTRY_DEV }}/${{ vars.APP_IMAGE_NAME_DEV }}:${{ steps.tag.outputs.tag }}
            ${{ vars.APP_REGISTRY_DEV }}/${{ vars.APP_IMAGE_NAME_DEV }}:${{ steps.tag.outputs.tag_sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy:
    name: Deploy ke server
    needs: build-push
    runs-on: ubuntu-latest
    environment: development
    timeout-minutes: 15
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Susun berkas env
        run: scripts/render-env.sh DEV ./keluaran
        env:
          APP_IMAGE_TAG: ${{ needs.build-push.outputs.tag }}

          SPRING_PROFILES_ACTIVE_DEV: ${{ vars.SPRING_PROFILES_ACTIVE_DEV }}
          APP_SERVER_PORT_DEV: ${{ vars.APP_SERVER_PORT_DEV }}
          APP_HOST_PORT_DEV: ${{ vars.APP_HOST_PORT_DEV }}
          APP_REGISTRY_DEV: ${{ vars.APP_REGISTRY_DEV }}
          APP_IMAGE_NAME_DEV: ${{ vars.APP_IMAGE_NAME_DEV }}
          APP_CONTAINER_NAME_DEV: ${{ vars.APP_CONTAINER_NAME_DEV }}
          APP_DATA_DIR_DEV: ${{ vars.APP_DATA_DIR_DEV }}
          APP_IMAGE_BASE_DIR_DEV: ${{ vars.APP_IMAGE_BASE_DIR_DEV }}
          APP_CORS_ALLOWED_ORIGINS_DEV: ${{ vars.APP_CORS_ALLOWED_ORIGINS_DEV }}
          APP_REDIS_KEY_PREFIX_DEV: ${{ vars.APP_REDIS_KEY_PREFIX_DEV }}
          APP_TZ_DEV: ${{ vars.APP_TZ_DEV }}
          APP_JAVA_TOOL_OPTIONS_DEV: ${{ vars.APP_JAVA_TOOL_OPTIONS_DEV }}
          DB_SCHEMA_DEV: ${{ vars.DB_SCHEMA_DEV }}
          DUMMYJSON_BASE_URL_DEV: ${{ vars.DUMMYJSON_BASE_URL_DEV }}

          DB_URL_DEV: ${{ secrets.DB_URL_DEV }}
          DB_USERNAME_DEV: ${{ secrets.DB_USERNAME_DEV }}
          DB_PASSWORD_DEV: ${{ secrets.DB_PASSWORD_DEV }}
          REDIS_HOST_DEV: ${{ secrets.REDIS_HOST_DEV }}
          REDIS_PORT_DEV: ${{ secrets.REDIS_PORT_DEV }}
          REDIS_USERNAME_DEV: ${{ secrets.REDIS_USERNAME_DEV }}
          REDIS_PASSWORD_DEV: ${{ secrets.REDIS_PASSWORD_DEV }}
          JWT_SECRET_DEV: ${{ secrets.JWT_SECRET_DEV }}
          CRYPTO_KEY_DEV: ${{ secrets.CRYPTO_KEY_DEV }}
          DEMO_PASSWORD_DEV: ${{ secrets.DEMO_PASSWORD_DEV }}

      - name: Siapkan kunci SSH
        env:
          SSH_PRIVATE_KEY: ${{ secrets.SSH_PRIVATE_KEY_DEV }}
          SSH_HOST: ${{ secrets.SSH_HOST_DEV }}
          SSH_PORT: ${{ secrets.SSH_PORT_DEV }}
        run: |
          mkdir -p ~/.ssh && chmod 700 ~/.ssh
          printf '%s\n' "$SSH_PRIVATE_KEY" > ~/.ssh/id_deploy
          chmod 600 ~/.ssh/id_deploy
          ssh-keyscan -p "$SSH_PORT" -H "$SSH_HOST" >> ~/.ssh/known_hosts 2>/dev/null

      - name: Kirim berkas deploy
        env:
          SSH_HOST: ${{ secrets.SSH_HOST_DEV }}
          SSH_PORT: ${{ secrets.SSH_PORT_DEV }}
          SSH_USERNAME: ${{ secrets.SSH_USERNAME_DEV }}
          DEPLOY_DIR: ${{ vars.APP_DEPLOY_DIR_DEV }}
        run: |
          ssh -i ~/.ssh/id_deploy -p "$SSH_PORT" "$SSH_USERNAME@$SSH_HOST" \
              "mkdir -p '$DEPLOY_DIR'"
          scp -i ~/.ssh/id_deploy -P "$SSH_PORT" \
              docker/docker-compose.yml \
              scripts/deploy.sh \
              keluaran/.env.deploy \
              keluaran/.env.app \
              "$SSH_USERNAME@$SSH_HOST:$DEPLOY_DIR/"

      - name: Jalankan deploy di server
        env:
          SSH_HOST: ${{ secrets.SSH_HOST_DEV }}
          SSH_PORT: ${{ secrets.SSH_PORT_DEV }}
          SSH_USERNAME: ${{ secrets.SSH_USERNAME_DEV }}
          DEPLOY_DIR: ${{ vars.APP_DEPLOY_DIR_DEV }}
        run: |
          ssh -i ~/.ssh/id_deploy -p "$SSH_PORT" "$SSH_USERNAME@$SSH_HOST" \
              "cd '$DEPLOY_DIR' && chmod 600 .env.app .env.deploy && chmod +x deploy.sh && ./deploy.sh"

      - name: Verifikasi dari luar server
        env:
          SSH_HOST: ${{ secrets.SSH_HOST_DEV }}
          HOST_PORT: ${{ vars.APP_HOST_PORT_DEV }}
        run: |
          kode="$(curl -fsS -o /dev/null -w '%{http_code}' --max-time 20 \
                  "http://$SSH_HOST:$HOST_PORT/api/rolemap")"
          echo "GET /api/rolemap -> $kode"
          [ "$kode" = "200" ]

      - name: Hapus kunci SSH
        if: always()
        run: rm -f ~/.ssh/id_deploy
```

---

## Kapan Workflow Ini Berjalan

Pemicunya adalah **push ke `main`**, dan merge pull request adalah push ke `main`.
Jadi pertanyaan "apakah deploy jalan saat PR di-merge" jawabannya ya, untuk
ketiga metode merge — merge commit, squash, maupun rebase. Tidak ada satu pun
yang lolos, karena ketiganya sama-sama menghasilkan commit baru di `main`.

| Momen | Event | Yang berjalan |
|---|---|---|
| PR dibuka, atau branch PR di-push lagi | `pull_request` | `ci.yml` saja — test. Tidak ada image, tidak ada deploy |
| PR di-merge ke `main` | `push` ke `main` | Workflow ini penuh: test → build & push → deploy |
| Push langsung ke `main` | `push` ke `main` | Workflow ini penuh |
| Actions → Run workflow | `workflow_dispatch` | Workflow ini penuh, dari branch yang dipilih |

**Test suite karena itu berjalan dua kali untuk satu perubahan** — sekali saat PR
dibuka, sekali lagi setelah di-merge. Ini disengaja. Commit hasil merge bukan
commit yang sama dengan head PR: pada squash merge ia commit yang benar-benar
baru, dan pada merge biasa ia memuat perubahan orang lain yang masuk ke `main`
setelah PR itu dibuka. Melewatkan test kedua berarti yang di-deploy adalah
sesuatu yang belum pernah diuji utuh.

Keduanya berbagi `concurrency.group: test-database`, jadi run kedua mengantre
dan tidak pernah bentrok dengan yang pertama di database yang sama.

Dua keadaan di mana merge **tidak** memicu workflow ini, keduanya di luar
kendali berkas ini tapi perlu diketahui:

- Merge dilakukan bot memakai `GITHUB_TOKEN` (misalnya action automerge). GitHub
  sengaja tidak memicu workflow dari push semacam itu untuk mencegah loop tak
  berujung. Merge lewat tombol di UI oleh manusia tetap memicu.
- Pesan commit memuat `[skip ci]`.

---

## Catatan Rancangan

**Kenapa `secrets: inherit` pada job `ci`.** Workflow CI membaca sendiri
secret bersuffix `_DEV` yang dibutuhkannya. Mendeklarasikan ulang satu per satu
lewat `secrets:` hanya menyalin daftar yang sama ke tempat kedua.

**Kenapa verifikasi terakhir dilakukan dari runner, bukan dari server.**
`deploy.sh` sudah memastikan container berstatus `healthy` — tapi itu dilihat
dari dalam server. Langkah `curl` di sini membuktikan port benar-benar
dipublikasikan dan bisa dicapai dari luar. Deploy yang "berhasil" tapi tidak bisa
diakses siapa pun bukan deploy yang berhasil.

**Kenapa `chmod 600` pada berkas env dijalankan lagi di server.** `scp` memakai
mode berkas asal, dan `render-env.sh` sudah memakai `umask 077`. Perintah itu
jaring pengaman yang murah — kalau salah satu asumsi berubah, kredensial tidak
terlanjur bisa dibaca seluruh user di server.

**Kenapa `rm -f ~/.ssh/id_deploy` di akhir.** Runner GitHub-hosted memang
dimusnahkan setelah job selesai, jadi ini murni kebiasaan yang harganya nol dan
tetap benar seandainya suatu saat pindah ke self-hosted runner.

**Kenapa tanggal dihitung dengan `TZ=Asia/Jakarta`.** Runner berjalan di UTC.
Deploy pukul 01:00 WIB akan menghasilkan tag bertanggal kemarin kalau zona
waktunya dibiarkan.

---

## Checklist

- [ ] `.github/workflows/deploy-dev.yml` dibuat
- [ ] Terpicu `push` ke `main` dan `workflow_dispatch`
- [ ] `concurrency.group: deploy-dev`, `cancel-in-progress: false`
- [ ] `permissions.packages: write` ada (tanpa ini push ke GHCR ditolak)
- [ ] Job `build-push` dan `deploy` memakai `needs` sehingga urutannya terjamin
- [ ] Dua tag di-push: `<ddMMyyyy>-latest` dan `<ddMMyyyy>-<sha7>`
- [ ] Seluruh variabel yang dibutuhkan `render-env.sh` ada di blok `env:`
- [ ] Tidak ada nilai kredensial tertulis langsung di YAML
- [ ] Langkah verifikasi `curl` gagal bila kode bukan `200`
- [ ] Kunci SSH dihapus dengan `if: always()`

## Verifikasi

1. Jalankan manual lewat **Actions → Deploy Development → Run workflow**.
2. Ketiga job harus hijau berurutan.
3. Di GHCR, package `demo1-dev` harus punya dua tag baru dengan tanggal hari ini.
4. Di server: `docker ps` menampilkan container `demo1-dev` berstatus `healthy`
   pada port `8080`.
5. Dari mesin mana pun: `curl -i http://<host>:8080/api/rolemap` mengembalikan
   `200`.
6. Uji gerbang CI: push commit yang merusak satu test ke `main`, pastikan job
   `build-push` **tidak** berjalan sama sekali dan tidak ada tag baru di GHCR.

Langkah 6 yang membuktikan CI benar-benar menjadi gerbang, bukan hiasan.

## Batasan Explicit

- **`ssh-keyscan` memercayai kunci host apa adanya pada koneksi pertama** (trust
  on first use). Selama jendela itu, deploy rentan terhadap serangan
  man-in-the-middle. Cara menutupnya adalah menyimpan fingerprint server sebagai
  secret `SSH_KNOWN_HOSTS_DEV` dan menuliskannya langsung, bukan memindainya.
  Tidak dilakukan sekarang demi menjaga jumlah secret tetap sedikit.
- **Tidak ada langkah yang membatalkan deploy bila verifikasi `curl` gagal.**
  Workflow ditandai merah, tapi container yang bermasalah tetap berjalan di
  server. Rollback manual dijelaskan di
  [impl-plan-scripts-deploy.md](./impl-plan-scripts-deploy.md).
- **Deploy dev dan deploy prod tidak saling menahan.** Keduanya punya
  `concurrency.group` sendiri, jadi bisa berjalan bersamaan. Job `ci` di dalam
  keduanya tetap bergiliran karena berbagi group `test-database`.
- **`vars.*` yang belum diisi di GitHub menghasilkan string kosong, bukan error.**
  Pemeriksaan variabel wajib di `render-env.sh` yang menangkap itu, dan itulah
  alasan pemeriksaannya ada.
