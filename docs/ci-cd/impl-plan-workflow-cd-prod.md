# Impl Plan — Workflow CD Production

`.github/workflows/deploy-prod.yml` — terpicu oleh push ke branch `production`
dan bisa dijalankan manual.

Isinya sama persis dengan
[deploy-dev.yml](./impl-plan-workflow-cd-dev.md) kecuali tujuh hal yang
didaftarkan di bawah. Dokumen ini hanya memuat selisihnya, supaya tidak ada dua
salinan YAML panjang yang bisa saling menyimpang di repositori.

---

## Cara Membuatnya

Salin `deploy-dev.yml` menjadi `deploy-prod.yml`, ganti seluruh `_DEV` menjadi
`_PROD`, lalu terapkan tujuh perubahan berikut. Enam yang pertama **tidak**
tertangani oleh penggantian suffix — ketinggalan satu saja berarti deploy prod
menimpa container dev.

| # | Bagian | deploy-dev.yml | deploy-prod.yml |
|---|---|---|---|
| 1 | `name` | `Deploy Development` | `Deploy Production` |
| 2 | `on.push.branches` | `[main]` | `[production]` |
| 3 | `concurrency.group` | `deploy-dev` | `deploy-prod` |
| 4 | `environment` (dua job) | `development` | `production` |
| 5 | Argumen `render-env.sh` | `DEV ./keluaran` | `PROD ./keluaran` |
| 6 | Nama step render | `Susun berkas env` | tetap |
| 7 | Seluruh `_DEV` lainnya | `_DEV` | `_PROD` |

Perubahan nomor 5 adalah yang paling mudah terlewat karena bukan bagian dari nama
variabel:

```yaml
      - name: Susun berkas env
        run: scripts/render-env.sh PROD ./keluaran
```

Ketiadaan `vars.APP_HOST_PORT_PROD` yang berbeda dari dev juga akan langsung
terasa: kedua container akan memperebutkan port `8080` dan yang kedua gagal start
dengan `address already in use`.

---

## Branch `production`

Pemicunya sama seperti dev, hanya branch-nya yang berbeda: **merge pull request
ke `production` adalah push ke `production`, jadi deploy prod ikut berjalan
sampai ke server.** Tabel lengkap kapan workflow berjalan ada di
[impl-plan-workflow-cd-dev.md](./impl-plan-workflow-cd-dev.md) dan berlaku sama
di sini. Kalau merge ke prod tidak boleh langsung men-deploy, yang dipakai adalah
required reviewer di environment `production` — lihat bagian di bawah.

Branch ini belum ada dan sengaja dibiarkan. Workflow yang menunjuk branch tidak
ada tidak menyebabkan error apa pun — GitHub hanya tidak pernah memicunya, dan
`workflow_dispatch` tetap bisa dipakai untuk deploy manual dari branch mana pun.

Saat branch dibuat nanti:

```bash
git checkout -b production
git push -u origin production
```

Push pertama itu langsung memicu workflow ini.

---

## Proteksi Environment (opsional, disarankan)

Job `build-push` dan `deploy` menunjuk `environment: production`. Selama
environment itu belum dibuat di GitHub, penunjukan ini tidak berpengaruh apa-apa.
Begitu dibuat di **Settings → Environments → New environment → `production`**,
dua pengaturan langsung berguna:

- **Required reviewers** — deploy prod berhenti dan menunggu persetujuan sebelum
  menjalankan job. Test dan build tetap jalan; yang tertahan hanya deploy.
- **Deployment branches** — batasi ke branch `production` saja, sehingga
  `workflow_dispatch` dari branch lain tidak bisa mengirim apa pun ke prod.

Keduanya tidak wajib untuk membuat pipeline ini jalan, jadi bisa ditambahkan
kapan saja tanpa mengubah berkas workflow.

---

## Checklist

- [ ] `.github/workflows/deploy-prod.yml` dibuat
- [ ] `name` diubah menjadi `Deploy Production`
- [ ] Terpicu `push` ke `production` dan `workflow_dispatch`
- [ ] `concurrency.group: deploy-prod`
- [ ] Kedua job memakai `environment: production`
- [ ] `render-env.sh` dipanggil dengan argumen `PROD`
- [ ] Tidak ada satu pun sisa `_DEV` di dalam berkas
- [ ] `vars.APP_HOST_PORT_PROD` bernilai `8081`, berbeda dari dev

Verifikasi tidak ada suffix yang tertinggal:

```bash
grep -c "_DEV" .github/workflows/deploy-prod.yml    # harus 0
grep -c "deploy-dev" .github/workflows/deploy-prod.yml  # harus 0
```

Verifikasi kedua berkas tidak menyimpang di luar tujuh titik di atas:

```bash
diff <(sed -e 's/_DEV/_X/g' -e 's/dev/X/g'  .github/workflows/deploy-dev.yml) \
     <(sed -e 's/_PROD/_X/g' -e 's/prod/X/g' .github/workflows/deploy-prod.yml)
```

Perbedaan yang muncul harus bisa dijelaskan oleh tabel di atas. Yang tidak bisa
dijelaskan berarti ada perubahan yang lupa disalin ke salah satu berkas.

## Verifikasi Ujung ke Ujung

1. Buat branch `production` dan push.
2. Ketiga job harus hijau.
3. Di GHCR muncul package `demo1-prod` dengan dua tag bertanggal hari ini.
4. Di server, `docker ps` menampilkan **dua** container: `demo1-dev` di port 8080
   dan `demo1-prod` di port 8081, keduanya `healthy`.
5. `curl http://<host>:8081/api/rolemap` mengembalikan `200`.
6. Pastikan container dev tidak terganggu: `curl http://<host>:8080/api/rolemap`
   tetap `200` dan `docker inspect` menunjukkan container dev tidak di-restart.

Langkah 6 yang membuktikan pemisahan antar profile benar-benar berlaku.

## Batasan Explicit

- **Production menunjuk database, Redis, dan server fisik yang sama dengan dev.**
  Ini keputusan yang diambil sadar untuk saat ini. Artinya "deploy production"
  belum berarti lingkungan yang terisolasi — deploy dev yang salah tetap bisa
  merusak data yang dilihat production.
- **Tidak ada perlindungan tambahan pada branch `production`.** Siapa pun yang
  bisa push ke branch itu bisa men-deploy ke prod. Branch protection rule dan
  required reviewer di environment adalah dua hal yang menutupnya, keduanya di
  luar berkas workflow.
- **Duplikasi YAML antara dev dan prod diterima.** Alternatifnya adalah satu
  workflow dengan matrix atau reusable workflow berparameter, yang membuat kedua
  jalur berbagi satu berkas — lebih ringkas, tapi juga berarti kesalahan pada
  berkas itu langsung mengenai production. Untuk dua lingkungan, dua berkas
  eksplisit lebih mudah dibaca dan lebih sulit disalahgunakan. Perintah `diff` di
  atas ada untuk menahan biayanya.
