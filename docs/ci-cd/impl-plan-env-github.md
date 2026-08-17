# Impl Plan — `.env.server.*` dan Pemetaan ke GitHub

Menulis ulang `.env.server.dev` dan `.env.server.prod` menjadi daftar lengkap
variabel yang dibutuhkan workflow dan runtime, lalu memetakannya ke GitHub
Actions Variables dan Secrets.

Kedua berkas ini adalah sumber kebenaran. Semua dokumen lain hanya membaca nama
variabel dari sini.

---

## Aturan Penamaan

**Setiap variabel bersuffix `_DEV` atau `_PROD` di GitHub, dan tanpa suffix
begitu sampai ke proses Java.** Suffix adalah alat untuk memisahkan *nilai* dev
dan prod di satu tempat penyimpanan yang sama; ia bukan bagian dari nama yang
dibaca aplikasi.

Pelucutannya terjadi di satu tempat saja, `scripts/render-env.sh` saat menyusun
`.env.app` — lihat
[impl-plan-scripts-deploy.md](./impl-plan-scripts-deploy.md).

Konsekuensinya yang paling berharga: `application.properties` memakai satu set
placeholder tanpa suffix yang melayani **keempat** skenario — container dev,
container prod, `./mvnw test`, dan `./mvnw spring-boot:run` lokal. Berkas
`application-dev.properties` dan `application-prod.properties` menyusut menjadi
hanya perbedaan perilaku yang sesungguhnya, tanpa satu pun kredensial. Rincian
di
[impl-plan-application-properties-profiles.md](./impl-plan-application-properties-profiles.md).

**Tiga variabel tidak sekadar dilucuti tapi berganti nama**, karena nama
tujuannya ditentukan oleh pihak lain dan tidak bisa diubah:

| Di GitHub | Di `.env.app` | Ditentukan oleh | Alasan |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE_<S>` | `SPRING_PROFILES_ACTIVE` | Spring Boot | Nama kunci baku untuk mengaktifkan profile. `SPRING_PROFILES_ACTIVE_DEV` tidak dikenali Spring dan aplikasi akan jalan dengan profile default |
| `APP_TZ_<S>` | `TZ` | glibc/musl | Nama baku zona waktu proses |
| `APP_JAVA_TOOL_OPTIONS_<S>` | `JAVA_TOOL_OPTIONS` | JVM | Nama baku opsi JVM |

Selebihnya cukup kehilangan suffixnya: `DB_URL_DEV` menjadi `DB_URL`,
`APP_REDIS_KEY_PREFIX_PROD` menjadi `APP_REDIS_KEY_PREFIX`, dan seterusnya.

---

## Inventaris Lengkap

Daftar ini adalah acuan penuh untuk penggantian menyeluruh isi Variables dan
Secrets di GitHub. **15 variables + 14 secrets per profile, 58 entri untuk
kedua profile.** Tidak ada nama lain yang dibaca workflow mana pun; nama di luar
daftar ini yang masih tersisa di GitHub adalah sisa yang harus dihapus.

Kolom "Dipakai" menunjukkan siapa yang membacanya — salah menempatkan sebuah nama
di tab yang keliru tidak menghasilkan error, hanya nilai kosong yang baru
ketahuan saat deploy berjalan setengah jalan.

### Variables (15 per profile)

| Nama | Dipakai | dev | prod |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE_<S>` | render-env | `dev` | `prod` |
| `APP_SERVER_PORT_<S>` | render-env | `8080` | `8080` |
| `APP_HOST_PORT_<S>` | render-env, verifikasi curl | `8080` | `8081` |
| `APP_REGISTRY_<S>` | render-env, build-push | `ghcr.io/masesas` | `ghcr.io/masesas` |
| `APP_IMAGE_NAME_<S>` | render-env, build-push | `demo1-dev` | `demo1-prod` |
| `APP_CONTAINER_NAME_<S>` | render-env | `demo1-dev` | `demo1-prod` |
| `APP_DEPLOY_DIR_<S>` | workflow (scp & ssh) | `/home/masesas/apps/demo1-dev` | `/home/masesas/apps/demo1-prod` |
| `APP_DATA_DIR_<S>` | render-env | `/home/masesas/data/demo1-dev/images` | `/home/masesas/data/demo1-prod/images` |
| `APP_IMAGE_BASE_DIR_<S>` | render-env | `/app/resources/images` | `/app/resources/images` |
| `APP_CORS_ALLOWED_ORIGINS_<S>` | render-env | `http://localhost:3000,http://localhost:5173` | sama |
| `APP_REDIS_KEY_PREFIX_<S>` | render-env | `demo:dev` | `demo:prod` |
| `APP_TZ_<S>` | render-env | `Asia/Jakarta` | `Asia/Jakarta` |
| `APP_JAVA_TOOL_OPTIONS_<S>` | render-env | `-XX:MaxRAMPercentage=75` | sama |
| `DB_SCHEMA_<S>` | render-env, ci.yml | `masesas` | `masesas` |
| `DUMMYJSON_BASE_URL_<S>` | render-env | `https://dummyjson.com` | sama |

`APP_DEPLOY_DIR_<S>` adalah satu-satunya variable yang **tidak** dibaca
`render-env.sh` — nilainya dipakai langsung oleh langkah `scp` dan `ssh` di
workflow. Karena itu ia juga tidak muncul di daftar variabel wajib script
tersebut, dan itu memang benar, bukan kelalaian.

### Secrets (14 per profile)

| Nama | Dipakai | Catatan |
|---|---|---|
| `SSH_HOST_<S>` | workflow deploy | juga dipakai langkah verifikasi curl |
| `SSH_PORT_<S>` | workflow deploy | |
| `SSH_USERNAME_<S>` | workflow deploy | |
| `SSH_PRIVATE_KEY_<S>` | workflow deploy | **bernilai multi-baris** — diimpor terpisah, lihat di bawah |
| `DB_URL_<S>` | render-env, ci.yml | |
| `DB_USERNAME_<S>` | render-env, ci.yml | |
| `DB_PASSWORD_<S>` | render-env, ci.yml | |
| `REDIS_HOST_<S>` | render-env, ci.yml | |
| `REDIS_PORT_<S>` | render-env, ci.yml | |
| `REDIS_USERNAME_<S>` | render-env, ci.yml | |
| `REDIS_PASSWORD_<S>` | render-env, ci.yml | |
| `JWT_SECRET_<S>` | render-env, ci.yml | |
| `CRYPTO_KEY_<S>` | render-env, ci.yml | wajib identik dev, prod, dan `.env` lokal |
| `DEMO_PASSWORD_<S>` | render-env, ci.yml | |

`ci.yml` membaca sepuluh secret bersuffix `_DEV` saja, bahkan ketika dipanggil
workflow deploy production — alasannya di
[impl-plan-workflow-ci.md](./impl-plan-workflow-ci.md).

---

## `.env.server.dev`

```properties
# ============================================================================
#  .env.server.dev — seluruh variabel deploy & runtime untuk profile dev
#
#  Berkas ini TIDAK di-commit (lihat .gitignore). Isinya disalin ke GitHub:
#  blok "variables" ke Settings > Secrets and variables > Actions > Variables,
#  blok "secret" ke tab Secrets. Nama di GitHub sama persis dengan nama di sini.
#
#  Environment GitHub: development
# ============================================================================

# ===== variables =====

# Profile Spring yang diaktifkan container.
SPRING_PROFILES_ACTIVE_DEV=dev

# Port yang didengarkan aplikasi DI DALAM container.
APP_SERVER_PORT_DEV=8080

# Port yang dipublikasikan ke host server. Dev 8080, prod 8081 — keduanya
# berjalan bersamaan di mesin yang sama, jadi tidak boleh sama.
APP_HOST_PORT_DEV=8080

# Registry dan nama image. Tag ditentukan workflow: <ddMMyyyy>-latest.
APP_REGISTRY_DEV=ghcr.io/masesas
APP_IMAGE_NAME_DEV=demo1-dev

# Nama container mengikuti pola {nama_container}-{profile}.
APP_CONTAINER_NAME_DEV=demo1-dev

# Direktori di server tempat docker-compose.yml, deploy.sh, dan berkas env
# diletakkan. Dibuat oleh deploy.sh kalau belum ada.
APP_DEPLOY_DIR_DEV=/home/masesas/apps/demo1-dev

# Direktori di HOST untuk berkas unggahan (avatar karyawan). Di-mount sebagai
# volume supaya tidak hilang saat container diganti.
APP_DATA_DIR_DEV=/home/masesas/data/demo1-dev/images

# Direktori yang sama dilihat DARI DALAM container. Dibaca app.image.base-dir.
APP_IMAGE_BASE_DIR_DEV=/app/resources/images

# Origin yang diizinkan CORS. Sebelumnya hardcode di application.properties.
APP_CORS_ALLOWED_ORIGINS_DEV=http://localhost:3000,http://localhost:5173

# Prefix key Redis. WAJIB berbeda antara dev dan prod — keduanya memakai satu
# instance Redis yang sama, dan tanpa prefix berbeda cache akan saling menimpa.
APP_REDIS_KEY_PREFIX_DEV=demo:dev

# Zona waktu proses container.
APP_TZ_DEV=Asia/Jakarta

# Opsi JVM. MaxRAMPercentage membuat heap mengikuti limit container,
# bukan memori fisik server.
APP_JAVA_TOOL_OPTIONS_DEV=-XX:MaxRAMPercentage=75

# Schema PostgreSQL.
DB_SCHEMA_DEV=masesas

# API publik tanpa kredensial.
DUMMYJSON_BASE_URL_DEV=https://dummyjson.com

# ===== secret =====

# Akses SSH ke server tujuan deploy.
SSH_HOST_DEV=129.226.195.9
SSH_PORT_DEV=22
SSH_USERNAME_DEV=masesas
# Nilainya multi-baris dan TIDAK ditulis di berkas ini — dotenv tidak bisa
# diandalkan membawanya utuh. Baris ini sengaja dibiarkan sebagai penanda bahwa
# secret-nya ada, dan diimpor terpisah langsung dari berkas kuncinya:
#     gh secret set SSH_PRIVATE_KEY_DEV < ~/.ssh/demo1_deploy
SSH_PRIVATE_KEY_DEV=DIIMPOR_TERPISAH_DARI_BERKAS_KUNCI

# Kunci penandatangan JWT. Regenerate dengan: openssl rand -base64 48
# Tanpa tanda kutip, tanpa karakter di luar alfabet Base64.
JWT_SECRET_DEV=GANTI_DENGAN_HASIL_openssl_rand_base64_48

# Kunci AES-256 Base64 untuk nik/npwp. WAJIB sama dengan .env lokal dan dengan
# .env.server.prod — data lama tidak bisa didekripsi dengan kunci lain.
CRYPTO_KEY_DEV=np1iEu6vzSPjj+5KveQmFWE+zQ/bM3DuNJKDCkVKYZY=

# Password bersama akun demo di database.
DEMO_PASSWORD_DEV=password123

DB_URL_DEV=jdbc:postgresql://129.226.195.9:5432/binar_finance
DB_USERNAME_DEV=binar_admin
DB_PASSWORD_DEV=binar_bc_password

REDIS_HOST_DEV=129.226.195.9
REDIS_PORT_DEV=6379
REDIS_USERNAME_DEV=binar_app
REDIS_PASSWORD_DEV=binar_bc_password
```

## `.env.server.prod`

Isinya sama persis dengan `.env.server.dev` kecuali empat baris. Sesuai
permintaan, nilainya memang identik untuk saat ini — yang berbeda hanya
penamaan, port, dan prefix cache.

```properties
# ===== variables =====
SPRING_PROFILES_ACTIVE_PROD=prod
APP_SERVER_PORT_PROD=8080
APP_HOST_PORT_PROD=8081
APP_REGISTRY_PROD=ghcr.io/masesas
APP_IMAGE_NAME_PROD=demo1-prod
APP_CONTAINER_NAME_PROD=demo1-prod
APP_DEPLOY_DIR_PROD=/home/masesas/apps/demo1-prod
APP_DATA_DIR_PROD=/home/masesas/data/demo1-prod/images
APP_IMAGE_BASE_DIR_PROD=/app/resources/images
APP_CORS_ALLOWED_ORIGINS_PROD=http://localhost:3000,http://localhost:5173
APP_REDIS_KEY_PREFIX_PROD=demo:prod
APP_TZ_PROD=Asia/Jakarta
APP_JAVA_TOOL_OPTIONS_PROD=-XX:MaxRAMPercentage=75
DB_SCHEMA_PROD=masesas
DUMMYJSON_BASE_URL_PROD=https://dummyjson.com

# ===== secret =====
SSH_HOST_PROD=129.226.195.9
SSH_PORT_PROD=22
SSH_USERNAME_PROD=masesas
# Multi-baris, diimpor terpisah:
#     gh secret set SSH_PRIVATE_KEY_PROD < ~/.ssh/demo1_deploy
SSH_PRIVATE_KEY_PROD=DIIMPOR_TERPISAH_DARI_BERKAS_KUNCI

JWT_SECRET_PROD=GANTI_DENGAN_HASIL_openssl_rand_base64_48
CRYPTO_KEY_PROD=np1iEu6vzSPjj+5KveQmFWE+zQ/bM3DuNJKDCkVKYZY=
DEMO_PASSWORD_PROD=password123

DB_URL_PROD=jdbc:postgresql://129.226.195.9:5432/binar_finance
DB_USERNAME_PROD=binar_admin
DB_PASSWORD_PROD=binar_bc_password

REDIS_HOST_PROD=129.226.195.9
REDIS_PORT_PROD=6379
REDIS_USERNAME_PROD=binar_app
REDIS_PASSWORD_PROD=binar_bc_password
```

Perbedaan dev vs prod, lengkap:

| Variabel | dev | prod |
|---|---|---|
| `SPRING_PROFILES_ACTIVE_*` | `dev` | `prod` |
| `APP_HOST_PORT_*` | `8080` | `8081` |
| `APP_IMAGE_NAME_*` | `demo1-dev` | `demo1-prod` |
| `APP_CONTAINER_NAME_*` | `demo1-dev` | `demo1-prod` |
| `APP_DEPLOY_DIR_*` | `.../apps/demo1-dev` | `.../apps/demo1-prod` |
| `APP_DATA_DIR_*` | `.../data/demo1-dev/images` | `.../data/demo1-prod/images` |
| `APP_REDIS_KEY_PREFIX_*` | `demo:dev` | `demo:prod` |

Selebihnya bernilai sama.

---

## Mengisi ke GitHub lewat `gh` CLI

Struktur kedua berkas dirancang supaya bisa diimpor langsung: penanda
`# ===== variables =====` dan `# ===== secret =====` menjadi batas pemisah, dan
`gh` mengabaikan baris komentar maupun baris kosong.

Karena nama sudah dibedakan lewat suffix, seluruhnya disimpan di level
repositori — tidak perlu GitHub Environment. Environment tetap dipakai untuk hal
lain: `deploy-prod.yml` menunjuk environment `production` supaya bisa diberi
required reviewer kalau nanti dibutuhkan
(lihat [impl-plan-workflow-cd-prod.md](./impl-plan-workflow-cd-prod.md)).

### 0. Prasyarat

```bash
gh auth status                       # harus login dengan akses admin repo
gh repo view masesas/spring-basic-sp --json name
```

Seluruh perintah di bawah dijalankan dari root repositori supaya `gh` menemukan
repo tujuannya sendiri. Kalau dijalankan dari tempat lain, tambahkan
`-R masesas/spring-basic-sp` di setiap perintah.

### 1. Pisahkan blok variables dan secret

```bash
pisah() {
    berkas="$1"; blok="$2"
    case "$blok" in
        variables) awk '/^# ===== variables =====/{a=1;next} /^# ===== secret =====/{a=0} a' "$berkas" ;;
        secret)    awk '/^# ===== secret =====/{a=1;next} a' "$berkas" ;;
    esac | grep -E '^[A-Z][A-Z0-9_]*=' | grep -v '^SSH_PRIVATE_KEY_'
}

umask 077
pisah .env.server.dev  variables > /tmp/gh-vars-dev.env
pisah .env.server.dev  secret    > /tmp/gh-secrets-dev.env
pisah .env.server.prod variables > /tmp/gh-vars-prod.env
pisah .env.server.prod secret    > /tmp/gh-secrets-prod.env

# Wajib 15 dan 13 (13 = 14 secret dikurangi SSH_PRIVATE_KEY yang disaring)
wc -l /tmp/gh-*.env
```

`SSH_PRIVATE_KEY_*` sengaja disaring keluar — nilainya multi-baris dan diimpor
tersendiri di langkah 3.

### 2. Impor variables dan secrets

```bash
gh variable set --env-file /tmp/gh-vars-dev.env
gh variable set --env-file /tmp/gh-vars-prod.env

gh secret set --env-file /tmp/gh-secrets-dev.env
gh secret set --env-file /tmp/gh-secrets-prod.env
```

Nama yang sudah ada akan ditimpa nilainya — itu memang yang diinginkan pada
penggantian menyeluruh.

### 3. Impor kunci SSH langsung dari berkasnya

```bash
gh secret set SSH_PRIVATE_KEY_DEV  < ~/.ssh/demo1_deploy
gh secret set SSH_PRIVATE_KEY_PROD < ~/.ssh/demo1_deploy
```

Yang dikirim adalah berkas **tanpa** akhiran `.pub`. Menyalurkannya lewat
`<` membuat isinya masuk utuh berikut baris barunya, tanpa melewati parser
dotenv sama sekali.

### 4. Hapus sisa yang tidak ada di daftar

Penggantian menyeluruh belum selesai selama nama lama masih tertinggal — nama
yang tidak dibaca workflow mana pun tidak berbahaya, tapi menyesatkan orang yang
membaca daftarnya nanti.

```bash
sah() { cat /tmp/gh-vars-dev.env /tmp/gh-vars-prod.env \
             /tmp/gh-secrets-dev.env /tmp/gh-secrets-prod.env \
        | sed 's/=.*//'; printf 'SSH_PRIVATE_KEY_DEV\nSSH_PRIVATE_KEY_PROD\n'; }

comm -23 <(gh variable list --json name -q '.[].name' | sort) <(sah | sort)
comm -23 <(gh secret   list --json name -q '.[].name' | sort) <(sah | sort)
```

Periksa keluarannya lebih dulu. Kalau memang sisa yang harus dibuang:

```bash
comm -23 <(gh variable list --json name -q '.[].name' | sort) <(sah | sort) \
  | xargs -r -n1 gh variable delete
comm -23 <(gh secret   list --json name -q '.[].name' | sort) <(sah | sort) \
  | xargs -r -n1 gh secret delete
```

### 5. Bersihkan berkas sementara

```bash
shred -u /tmp/gh-*.env 2>/dev/null || rm -f /tmp/gh-*.env
```

Empat berkas itu memuat kredensial dalam bentuk terbaca. `/tmp` bukan tempatnya
menginap.

### 6. Verifikasi

```bash
gh variable list | wc -l          # harus 30 (15 dev + 15 prod)
gh secret   list | wc -l          # harus 28 (14 dev + 14 prod)

# Tidak boleh ada secret yang isinya masih penanda
gh secret list --json name -q '.[].name' | grep -c SSH_PRIVATE_KEY   # harus 2
```

`gh` tidak pernah menampilkan nilai secret, jadi kebenaran isinya tidak bisa
diperiksa dari sini. Yang membuktikannya adalah workflow CI berjalan hijau
(kredensial database benar) dan deploy berhasil (kunci SSH benar).

Kalau lebih suka lewat antarmuka web, isinya sama: **Settings → Secrets and
variables → Actions**, baris blok variables ke tab Variables, baris blok secret
ke tab Secrets, nama dan nilai apa adanya tanpa tanda kutip.

---

## `.env.server.example`

`.env.server.dev` dan `.env.server.prod` tidak ikut di-commit, jadi tidak ada
jejak di repositori tentang variabel apa saja yang dibutuhkan deploy. Perbaikannya
sama seperti yang sudah dilakukan `.env.example` untuk `.env`: tambahkan
`.env.server.example` berisi seluruh nama variabel dengan nilai placeholder.

Perlu satu baris pengecualian di `.gitignore` karena pola `.env.server.*`
sekarang menelan berkas ini juga:

```gitignore
### Kredensial — jangan pernah di-commit ###
.env
.env.local
.env.server.*
!.env.example
!.env.server.example
```

Isinya persis struktur di atas dengan seluruh nilai diganti placeholder,
memakai suffix `_DEV` sebagai contoh dan catatan bahwa versi prod memakai
`_PROD`.

---

## Checklist

- [ ] `.env.server.dev` ditulis ulang dengan seluruh variabel di atas
- [ ] `.env.server.prod` ditulis ulang, seluruh suffix `_PROD`, tidak ada sisa `_DEV`
- [ ] `CRYPTO_KEY_DEV` dan `CRYPTO_KEY_PROD` identik dengan `.env` lokal
- [ ] `JWT_SECRET_DEV` dan `JWT_SECRET_PROD` diregenerate, tanpa tanda kutip
- [ ] `SSH_PRIVATE_KEY_DEV` dan `SSH_PRIVATE_KEY_PROD` diisi private key nyata
- [ ] `APP_HOST_PORT_DEV` ≠ `APP_HOST_PORT_PROD`
- [ ] `APP_REDIS_KEY_PREFIX_DEV` ≠ `APP_REDIS_KEY_PREFIX_PROD`
- [ ] Blok penanda `# ===== variables =====` dan `# ===== secret =====` ada di kedua berkas dan ejaannya persis sama
- [ ] Pemisahan menghasilkan 15 variables dan 13 secrets per profile (13 = 14 dikurangi `SSH_PRIVATE_KEY_*`)
- [ ] `gh variable set --env-file` dijalankan untuk dev dan prod
- [ ] `gh secret set --env-file` dijalankan untuk dev dan prod
- [ ] `SSH_PRIVATE_KEY_DEV` dan `SSH_PRIVATE_KEY_PROD` diimpor terpisah dari berkas kunci, bukan dari dotenv
- [ ] Nama sisa di luar inventaris sudah dihapus dari Variables dan Secrets
- [ ] `gh variable list` berjumlah 30, `gh secret list` berjumlah 28
- [ ] Berkas sementara di `/tmp` sudah dihapus
- [ ] `.env.server.example` dibuat dan `.gitignore` diberi baris pengecualian
- [ ] `git status` tidak menampilkan `.env.server.dev` maupun `.env.server.prod`

## Verifikasi

```bash
# Tidak boleh ada sisa suffix DEV di berkas prod
grep -c "_DEV" .env.server.prod    # harus 0

# Kunci kripto harus identik di tiga berkas
grep "^CRYPTO_KEY" .env
grep "^CRYPTO_KEY_DEV" .env.server.dev
grep "^CRYPTO_KEY_PROD" .env.server.prod

# Kunci kripto harus 32 byte setelah di-decode
echo -n "np1iEu6vzSPjj+5KveQmFWE+zQ/bM3DuNJKDCkVKYZY=" | base64 -d | wc -c   # harus 32

# Berkas rahasia tidak boleh terlacak git
git check-ignore -v .env.server.dev .env.server.prod
```

## Batasan Explicit

- **Nilai multi-baris tidak bisa lewat `--env-file`.** Hanya
  `SSH_PRIVATE_KEY_*` yang bersifat begitu, dan karena itu ia disaring keluar dan
  diimpor tersendiri. Kalau nanti ada secret multi-baris baru, ia harus ikut
  disaring — kalau tidak, nilainya terpotong di baris pertama tanpa satu pun
  pesan kesalahan.
- **Baris `SSH_PRIVATE_KEY_*=DIIMPOR_TERPISAH_DARI_BERKAS_KUNCI` di berkas env
  adalah penanda, bukan nilai.** Ia sengaja dibiarkan supaya berkasnya tetap
  menjadi inventaris yang utuh. Konsekuensinya, siapa pun yang mengimpor tanpa
  penyaring `grep -v '^SSH_PRIVATE_KEY_'` akan mengunggah teks penanda itu
  sebagai secret, dan deploy gagal dengan `Permission denied (publickey)`.
- **`gh` menimpa tanpa bertanya.** `gh variable set` dan `gh secret set`
  langsung mengganti nilai yang sudah ada. Tidak ada konfirmasi dan tidak ada
  riwayat nilai lama — secret yang tertimpa nilai salah hanya bisa diperbaiki
  dengan mengisinya ulang dari sumber aslinya.
- **`gh` tidak pernah menampilkan nilai secret.** Kesalahan isi (bukan kesalahan
  nama) baru ketahuan saat CI atau deploy berjalan, bukan saat impor.
- **`SSH_HOST`, `SSH_PORT`, dan `SSH_USERNAME` diperlakukan sebagai secret**
  mengikuti penandaan yang sudah ada, walaupun secara teknis bukan rahasia
  kriptografis. Konsekuensinya nilainya tersamar di log Actions, yang justru
  menyulitkan saat mendiagnosa kegagalan koneksi.
- **Kredensial GHCR untuk server tidak disimpan di GitHub.** Server login sekali
  secara manual ke `ghcr.io` dan kredensialnya tersimpan di
  `~/.docker/config.json` — lihat
  [impl-plan-prasyarat-server.md](./impl-plan-prasyarat-server.md). Alasannya:
  token yang dikirim tiap deploy lewat SSH berisiko muncul di daftar proses
  server. Konsekuensinya, saat PAT server kedaluwarsa, deploy gagal di langkah
  `docker pull` dan harus diperbaiki manual di server.
- **`DB_SCHEMA` dan `REDIS_PORT` diklasifikasikan berbeda** — `DB_SCHEMA_*`
  sebagai variable, `REDIS_PORT_*` sebagai secret. Ini mengikuti penandaan yang
  sudah ada di berkas asli, bukan hasil penilaian ulang.
- Nilai dev dan prod sengaja identik untuk saat ini. Begitu prod punya database
  sendiri, `DB_URL_PROD`, `CRYPTO_KEY_PROD`, dan `JWT_SECRET_PROD` harus
  dipisahkan — dan `CRYPTO_KEY_PROD` hanya boleh berbeda kalau database prod
  memang kosong dari data terenkripsi.
