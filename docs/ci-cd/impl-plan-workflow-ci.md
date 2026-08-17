# Impl Plan — Workflow CI

`.github/workflows/ci.yml` — menjalankan seluruh test suite. Dipakai dua cara:
otomatis pada pull request, dan dipanggil oleh kedua workflow deploy sebagai
gerbang sebelum image dibangun.

---

## Kenapa Test Menembak Database Remote

Dari 18 kelas test, 12 memakai `@SpringBootTest` dan membutuhkan PostgreSQL serta
Redis yang hidup **beserta datanya** — test RBAC login memakai akun
`admin@masesas.test`, `marketing@masesas.test`, dan seterusnya yang berasal dari
`rbac_role_penuh_masesas.sql`.

Pilihan yang lebih rapi adalah service container PostgreSQL di dalam job, tapi itu
tidak mungkin sekarang: **DDL tabel inti tidak ada di repositori.** `CREATE TABLE`
hanya tersedia untuk `payroll_karyawan`, `role`, `karyawan_role`, `customer`, dan
`log_aktivitas`. Tabel `karyawan`, `detail_karyawan`, `rekening`, `training`, dan
`karyawan_training` tidak punya DDL sama sekali — `seeder.sql` hanya berisi
`INSERT`. Database kosong tidak bisa dibangun dari nol dengan isi repositori saat
ini.

Karena itu CI memakai PostgreSQL dan Redis yang sudah ada. Konsekuensinya
didaftarkan di bagian Batasan Explicit dan tidak diperhalus.

**CI selalu memakai kredensial bersuffix `_DEV`, termasuk saat dipanggil oleh
workflow deploy production.** Saat ini nilainya memang sama persis, jadi tidak ada
bedanya. Tapi begitu prod punya database sendiri, aturan ini tetap yang benar:
test menulis data dan tidak boleh menyentuh database production.

---

## `.github/workflows/ci.yml`

```yaml
name: CI

on:
  pull_request:
    branches: [main, production]
  workflow_call:

# Test menulis ke PostgreSQL yang dipakai bersama. Dua job test yang berjalan
# bersamaan akan saling mengacaukan data, jadi dijalankan bergiliran.
# cancel-in-progress false: job yang sedang jalan dibiarkan selesai supaya
# tidak meninggalkan data setengah jadi.
concurrency:
  group: test-database
  cancel-in-progress: false

jobs:
  test:
    name: Full test suite
    runs-on: ubuntu-latest
    timeout-minutes: 25

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Siapkan Java 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven

      - name: Jalankan seluruh test
        run: ./mvnw -B clean test
        env:
          DB_URL: ${{ secrets.DB_URL_DEV }}
          DB_USERNAME: ${{ secrets.DB_USERNAME_DEV }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD_DEV }}
          DB_SCHEMA: ${{ vars.DB_SCHEMA_DEV }}
          REDIS_HOST: ${{ secrets.REDIS_HOST_DEV }}
          REDIS_PORT: ${{ secrets.REDIS_PORT_DEV }}
          REDIS_USERNAME: ${{ secrets.REDIS_USERNAME_DEV }}
          REDIS_PASSWORD: ${{ secrets.REDIS_PASSWORD_DEV }}
          JWT_SECRET: ${{ secrets.JWT_SECRET_DEV }}
          CRYPTO_KEY: ${{ secrets.CRYPTO_KEY_DEV }}
          DEMO_PASSWORD: ${{ secrets.DEMO_PASSWORD_DEV }}
          # Prefix khusus CI supaya test tidak menimpa cache container dev
          # yang sedang melayani permintaan.
          APP_REDIS_KEY_PREFIX: demo:ci
          # Dibutuhkan application-local.properties, profile yang diaktifkan
          # Surefire. Test memakai MockMvc, jadi nilainya tidak pernah dipakai
          # membuka port — tapi placeholdernya wajib ter-resolve.
          APP_SERVER_PORT: 8080

      - name: Unggah laporan JaCoCo
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-${{ github.run_number }}
          path: target/site/jacoco/
          retention-days: 7

      - name: Unggah laporan Surefire saat gagal
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: surefire-${{ github.run_number }}
          path: target/surefire-reports/
          retention-days: 7
```

Beberapa hal yang disengaja:

- **Nama environment variable tanpa suffix.** Test berjalan dengan profile
  `local`, yang diaktifkan `maven-surefire-plugin` di `pom.xml` — bukan tanpa
  profile, karena `application.properties` tidak lagi memuat konfigurasi
  database maupun Redis. Berkas `application-local.properties` memakai
  placeholder tanpa suffix; pemetaan dari `_DEV` ke nama tanpa suffix terjadi di
  blok `env:` ini. Lihat
  [impl-plan-application-properties-profiles.md](./impl-plan-application-properties-profiles.md).
- **`APP_SERVER_PORT: 8080`.** Dituntut `application-local.properties` sebagai
  placeholder wajib tanpa default. Test memakai MockMvc sehingga port ini tidak
  pernah dibuka, tapi placeholdernya tetap harus ter-resolve.
- **`APP_REDIS_KEY_PREFIX: demo:ci`.** Satu-satunya nilai yang sengaja berbeda dari
  dev. Tanpa ini, test akan menimpa cache container dev di Redis yang sama.
- **`./mvnw -B clean test`, bukan `verify`.** Fase `verify` memicu
  `dependency-check-maven` yang mengunduh basis data NVD — belasan menit, dan
  gagal karena alasan jaringan, bukan karena mutu kode. Pemindaian A06 tetap
  dijalankan manual seperti sebelumnya.
- **`cache: maven` dari `setup-java`.** Wrapper repositori ini bertipe
  `only-script` dan mengunduh distribusi Maven sendiri; cache menyimpan `~/.m2`
  sehingga dependency tidak diunduh ulang tiap run.
- **`timeout-minutes: 25`.** Test menembak database lintas internet; tanpa batas
  waktu, koneksi yang menggantung menahan runner sampai batas 6 jam.

---

## Checklist

- [ ] `.github/workflows/ci.yml` dibuat
- [ ] Terpicu pada pull request ke `main` dan `production`
- [ ] Bisa dipanggil lewat `workflow_call`
- [ ] Seluruh 13 nilai `env` disetel: 10 secret + 1 variable bersuffix `_DEV`, plus `APP_REDIS_KEY_PREFIX` dan `APP_SERVER_PORT` sebagai nilai literal
- [ ] `APP_REDIS_KEY_PREFIX` diisi `demo:ci` dan `APP_SERVER_PORT` diisi `8080`
- [ ] Test berjalan dengan profile `local` (dibuktikan baris `The following 1 profile is active: "local"` di log)
- [ ] `concurrency.group` disetel dan `cancel-in-progress: false`
- [ ] Laporan JaCoCo dan Surefire diunggah sebagai artifact
- [ ] Menjalankan `test`, bukan `verify`

## Verifikasi

1. Buat pull request percobaan ke `main`. Workflow CI harus berjalan dan hijau.
2. Buka artifact `jacoco-<n>`, pastikan `index.html` ada isinya.
3. Rusak satu test dengan sengaja, push, pastikan workflow **merah** dan artifact
   `surefire-<n>` memuat detail kegagalannya. Kembalikan setelah terbukti.
4. Kosongkan sementara secret `DB_PASSWORD_DEV`, pastikan job gagal saat koneksi
   database — bukan lolos diam-diam dengan nilai bawaan. Kembalikan setelahnya.

Langkah 4 adalah yang membuktikan perbaikan placeholder di
[impl-plan-perbaikan-konfigurasi.md](./impl-plan-perbaikan-konfigurasi.md) benar-benar
berlaku.

## Batasan Explicit

- **PostgreSQL dan Redis harus bisa dijangkau dari IP runner GitHub yang
  berubah-ubah.** Praktiknya berarti kedua port terbuka luas ke internet.
  Ini konsekuensi keamanan yang nyata dan diterima secara sadar untuk repositori
  latihan ini. Untuk lingkungan sungguhan, self-hosted runner di server yang sama
  menghilangkan kebutuhan itu sepenuhnya.
- **Test menulis ke database yang juga dipakai aplikasi berjalan.** Sebagian besar
  test RBAC hanya membaca, tapi `KaryawanControllerTest` melakukan create dan
  delete. Data uji bisa tertinggal di `binar_finance.masesas`.
- **`concurrency` hanya melindungi dari workflow di repositori ini.** `./mvnw
  test` yang dijalankan seseorang dari laptopnya pada saat bersamaan tetap bisa
  bentrok.
- **Pull request dari fork tidak mendapat secret** dan CI-nya akan gagal di
  langkah test. Repositori ini tidak menerima kontribusi dari fork, jadi tidak
  ditangani.
- **Cakupan JaCoCo tidak diperiksa ambang minimumnya.** `jacoco:check` terikat ke
  fase `verify` yang sengaja tidak dijalankan. Laporannya tetap dihasilkan dan
  diunggah untuk dibaca manual.
