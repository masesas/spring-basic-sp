# CI/CD — Indeks Implementation Plan

Kumpulan rencana implementasi CI/CD untuk `demo1`: build image di GitHub Actions,
kirim ke GHCR, jalankan container di server Ubuntu. Setiap dokumen berdiri sendiri
(atomic) dan hanya membahas satu berkas atau satu keputusan.

## Urutan Eksekusi

Kerjakan berurutan. Dokumen di bawah bergantung pada dokumen di atasnya.

| # | Dokumen | Menghasilkan |
|---|---|---|
| 1 | [impl-plan-perbaikan-konfigurasi.md](./impl-plan-perbaikan-konfigurasi.md) | Perbaikan blocker di `application.properties` & kunci kripto |
| 2 | [impl-plan-env-github.md](./impl-plan-env-github.md) | `.env.server.dev`, `.env.server.prod`, pemetaan ke GitHub Variables/Secrets |
| 3 | [impl-plan-application-properties-profiles.md](./impl-plan-application-properties-profiles.md) | `application-dev.properties`, `application-prod.properties` |
| 4 | [impl-plan-dockerfile.md](./impl-plan-dockerfile.md) | `docker/Dockerfile`, `.dockerignore` |
| 5 | [impl-plan-docker-compose.md](./impl-plan-docker-compose.md) | `docker/docker-compose.yml` |
| 6 | [impl-plan-scripts-deploy.md](./impl-plan-scripts-deploy.md) | `scripts/render-env.sh`, `scripts/deploy.sh` |
| 7 | [impl-plan-workflow-ci.md](./impl-plan-workflow-ci.md) | `.github/workflows/ci.yml` |
| 8 | [impl-plan-workflow-cd-dev.md](./impl-plan-workflow-cd-dev.md) | `.github/workflows/deploy-dev.yml` |
| 9 | [impl-plan-workflow-cd-prod.md](./impl-plan-workflow-cd-prod.md) | `.github/workflows/deploy-prod.yml` |
| 10 | [impl-plan-prasyarat-server.md](./impl-plan-prasyarat-server.md) | Langkah manual sekali jalan di server Ubuntu |

## Keputusan yang Mengikat Seluruh Dokumen

Nilai-nilai berikut dipakai konsisten di semua dokumen. Kalau salah satu berubah,
ubah di semua dokumen sekaligus.

| Hal | Development | Production |
|---|---|---|
| Branch pemicu | `main` | `production` |
| Profile Spring | `dev` | `prod` |
| Profile saat `./mvnw test` & `spring-boot:run` | `local` (diaktifkan di `pom.xml`) | sama |
| Registry | `ghcr.io/masesas` | `ghcr.io/masesas` |
| Nama image | `ghcr.io/masesas/demo1-dev` | `ghcr.io/masesas/demo1-prod` |
| Tag image | `<ddMMyyyy>-latest` + `<ddMMyyyy>-<sha7>` | sama |
| Nama container | `demo1-dev` | `demo1-prod` |
| Port host | `8080` | `8081` |
| Port container | `8080` | `8080` |
| Suffix env/secret di GitHub | `_DEV` | `_PROD` |
| Suffix saat sampai ke proses Java | dilucuti `render-env.sh` | sama |
| GitHub Environment | `development` | `production` |

Contoh tag lengkap untuk deploy tanggal 17 Agustus 2026:

```
ghcr.io/masesas/demo1-dev:17082026-latest
ghcr.io/masesas/demo1-dev:17082026-a1b2c3d
```

Tanggal dihitung memakai `TZ=Asia/Jakarta` supaya tidak meleset satu hari dari
tanggal lokal — runner GitHub berjalan di UTC.

## Alur Singkat

Pemicunya adalah push ke branch, dan **merge pull request adalah push** — jadi
merge PR ke `main` menjalankan deploy dev sampai ke server, merge ke
`production` menjalankan deploy prod. Selama PR masih terbuka, yang berjalan
hanya `ci.yml`. Rinciannya di
[impl-plan-workflow-cd-dev.md](./impl-plan-workflow-cd-dev.md).

```
push ke main / production  (termasuk lewat merge pull request)
        │
        ▼
   ci.yml  ── full test suite (./mvnw clean test) ke PostgreSQL & Redis remote
        │      gagal ⇒ berhenti, tidak ada image yang dibuat
        ▼
  build & push ── docker buildx build → ghcr.io/masesas/demo1-<profile>:<tag>
        │
        ▼
     deploy  ── scp docker-compose.yml + deploy.sh + .env.app + .env.deploy
        │       ssh → deploy.sh: docker pull → docker compose up -d → cek health
        ▼
    verifikasi ── GET /api/rolemap harus 200 dari host server
```

Yang dikirim ke server hanya empat berkas kecil. Source code tidak pernah menyentuh
server; server hanya menarik image yang sudah jadi dari GHCR.

## Batasan Explicit

Berlaku untuk seluruh rangkaian dokumen ini.

**Out of scope**

- Reverse proxy, TLS, dan domain. Aplikasi diakses langsung lewat port host.
- Rollback otomatis saat health check gagal. Rollback dilakukan manual dengan
  menjalankan `deploy.sh` memakai tag `<ddMMyyyy>-<sha7>` versi sebelumnya.
- Migrasi database otomatis. Berkas `.sql` tetap dijalankan manual.
- Blue-green atau zero-downtime deployment. Container lama dihentikan sebelum
  yang baru jalan, jadi ada jeda beberapa detik.
- PostgreSQL dan Redis di dalam compose. Keduanya sudah eksternal dan dikelola
  di luar repositori ini.

**Known limitations**

- Dev dan prod menunjuk PostgreSQL, Redis, dan server fisik yang sama. Deploy
  prod dan dev menulis ke `binar_finance.masesas` yang sama. Redis aman karena
  prefix key berbeda per profile, PostgreSQL tidak punya pemisahan apa pun.
- Test suite berjalan langsung ke database remote, jadi CI tidak bisa jalan
  paralel dua-duanya tanpa saling mengganggu. `concurrency` di workflow menahan
  hal ini, tapi hanya untuk workflow di repositori ini.
- Tag `<ddMMyyyy>-latest` tidak unik dalam satu hari. Tag kedua
  `<ddMMyyyy>-<sha7>` disediakan khusus supaya rollback tetap mungkin.
- DDL tabel inti (`karyawan`, `detail_karyawan`, `rekening`, `training`,
  `karyawan_training`) tidak ada di repositori. Selama itu belum ada, database
  bersih tidak bisa dibangun dari nol dan CI wajib memakai database remote.
