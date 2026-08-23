# Test HTTP per Peran (RBAC)

Satu berkas `.http` untuk tiap peran. Isinya bukan sekadar contoh request, tetapi
matriks akses yang bisa dijalankan: setiap endpoint dipanggil dengan token peran
tersebut, dan status yang diharapkan ditegakkan lewat `client.test(...)`.

Berkas ini memakai format JetBrains HTTP Client (IntelliJ IDEA, WebStorm) dan
bisa dijalankan lewat UI maupun `ijhttp` di terminal.

## Persiapan

1. Jalankan ketiga migrasi RBAC bila belum, berurutan:

   ```bash
   HASH="{bcrypt}$(htpasswd -bnBC 12 "" "$DEMO_PASSWORD" | tr -d ':\n')"
   psql -h <host> -U <user> -d binar_finance -v pwd_hash="$HASH" -f ../rbac_masesas.sql
   psql -h <host> -U <user> -d binar_finance -f ../rbac_role_penuh_masesas.sql
   psql -h <host> -U <user> -d binar_finance -f ../rbac_superadmin_masesas.sql
   ```

2. Salin berkas environment privat lalu isi passwordnya:

   ```bash
   cp http-client.private.env.json.example http-client.private.env.json
   ```

   Nilai `demoPassword` harus sama persis dengan `DEMO_PASSWORD` di `.env`.
   Berkas `http-client.private.env.json` tidak ikut di-commit.

3. Jalankan aplikasi:

   ```bash
   ./mvnw spring-boot:run
   ```

4. Pilih environment `dev` di IDE, lalu jalankan berkas peran yang diinginkan.

Dari terminal, tanpa perlu memasang apa pun:

```bash
python3 http/runner.py
```

`runner.py` menjalankan seluruh berkas `role-*.http` berurutan dan keluar dengan
kode 1 bila ada assertion yang gagal, jadi bisa dipakai langsung di CI. Ia
penafsir minimal — hanya menangani subset format yang dipakai berkas di sini,
bukan pengganti JetBrains HTTP Client. Berikan path direktori sebagai argumen
bila dijalankan dari tempat lain.

Bila `ijhttp` sudah terpasang, itu juga bisa dipakai:

```bash
ijhttp --env-file http-client.env.json \
       --private-env-file http-client.private.env.json \
       --env dev role-admin.http
```

## Daftar berkas

| Berkas | Akun | Peran |
|---|---|---|
| `role-superadmin.http` | superadmin@masesas.test (id 9) | SUPERADMIN |
| `role-admin.http` | admin@masesas.test (id 1) | ADMIN |
| `role-manager.http` | manager@masesas.test (id 2) | MANAGER |
| `role-marketing.http` | marketing@masesas.test (id 3) | MARKETING |
| `role-sales.http` | sales@masesas.test (id 4) | SALES |
| `role-hr.http` | hr@masesas.test (id 7) | HR |
| `role-karyawan.http` | karyawan@masesas.test (id 8) | KARYAWAN |
| `role-customer.http` | customer1@masesas.test | CUSTOMER |
| `role-multi-manager-sales.http` | manager.sales@masesas.test (id 5) | MANAGER + SALES |
| `role-tanpa-peran.http` | tanparole@masesas.test (id 6) | — |
| `role-guest.http` | tanpa token | GUEST (anonim) |
| `loan.http` | customer baru tiap jalan + ADMIN, MANAGER, MARKETING | alur & RBAC modul pinjaman |

Pendukung: `runner.py` (eksekusi dari terminal), `http-client.env.json`
(host, email, id akun), `http-client.private.env.json.example` (contoh password).

`loan.http` sengaja **tidak** memakai awalan `role-`, jadi `runner.py` tidak
menjalankannya. Alasannya: alur pinjaman mengubah plafond customer secara
permanen — dijalankan berulang di CI, plafond akan habis dan berkasnya mulai
gagal karena kehabisan kuota, bukan karena ada yang rusak. Jalankan manual dari
IDE bila ingin menelusuri alurnya.

## Matriks akses

`—` berarti 403 untuk pengguna yang sudah login, dan 401 untuk guest.

| Endpoint | SUPERADMIN | ADMIN | MANAGER | MARKETING | SALES | HR | KARYAWAN | CUSTOMER | Tanpa peran | Guest |
|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| `POST /api/karyawan` | ✅ | ✅ | ✅ | — | — | — | — | — | — | 401 |
| `GET /api/karyawan` *(dan `/{id}`, `/search`, `/all`)* | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — | — | 401 |
| `GET /api/karyawan/page` | ✅ | ✅ | — | — | — | — | — | — | — | 401 |
| `PUT /api/karyawan/{id}` | ✅ | ✅ | ✅ | — | — | — | — | — | — | 401 |
| `PUT/DELETE /api/karyawan/{id}/detail` | ✅ | ✅ | ✅ | — | — | — | — | — | — | 401 |
| `POST /api/karyawan/{id}/avatar` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — | — | 401 |
| `DELETE /api/karyawan/{id}` | ✅ | ✅ | — | — | — | — | — | — | — | 401 |
| `/api/payroll` *(selain DELETE)* | ✅ | ✅ | ✅ | — | — | ✅ | — | — | — | 401 |
| `DELETE /api/payroll/{id}/{periode}` | ✅ | ✅ | — | — | — | ✅ | — | — | — | 401 |
| `GET /api/customer/me` | 404 | — | — | — | — | — | — | ✅ | — | 401 |
| `GET /api/branch`, `/api/loan-product`, `/api/loan-document-type` | ✅ | ✅ | ✅ | ✅ | ✅ | — | — | — | — | 401 |
| `POST/PUT/DELETE` master pinjaman *(dan `/api/permission`, `/api/role-permission`)* | ✅ | ✅ | — | — | — | — | — | — | — | 401 |
| `GET /api/loan-plafond/**` | ✅ | ✅ | ✅ | — | ✅ | — | — | — | — | 401 |
| `PUT /api/loan-plafond` | ✅ | ✅ | — | — | — | — | — | — | — | 401 |
| `GET /api/loan-application` *(dan `/{id}`)* | ✅ | ✅ | ✅ | ✅ | ✅ | — | — | — | — | 401 |
| `POST /api/loan-application/{id}/approve` *(dan `/reject`)* | ✅ | ✅ | ✅ | — | — | — | — | — | — | 401 |
| `POST /api/loan-application/{id}/disburse` | ✅ | ✅ | — | — | — | — | — | — | — | 401 |
| `GET /api/loan-payment/**` | ✅ | ✅ | ✅ | — | ✅ | — | — | — | — | 401 |
| `POST /api/loan-payment` | ✅ | ✅ | — | — | ✅ | — | — | — | — | 401 |
| `/api/customer/loan-application/**` | 404 | — | — | — | — | — | — | ✅ | — | 401 |
| `/api/karyawan2/**`, `/api/sp/karyawan/**` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 401 |
| `/api/auth/**`, `/api/rolemap/**` | publik | publik | publik | publik | publik | publik | publik | publik | publik | publik |
| `/docs`, `/docs/**` | publik | publik | publik | publik | publik | publik | publik | publik | publik | publik |

`/docs` adalah dokumentasi API interaktif (Scalar) beserta spesifikasi
OpenAPI-nya di `/docs/openapi`. Keduanya ditangani `SecurityFilterChain`
tersendiri di `SecurityConfig` — bukan chain utama — karena halaman itu perlu
Content-Security-Policy yang lebih longgar daripada `/api/**`. Lihat
[../docs/api-docs/README.md](../docs/api-docs/README.md).

SUPERADMIN mendapat `404` di `GET /api/customer/me`, bukan `403` maupun `✅`:
otorisasinya lolos lewat hierarki peran, tetapi tidak ada baris `customer`
dengan email `superadmin@masesas.test`, jadi yang gagal adalah pencarian
datanya. Ini satu-satunya sel yang membedakan "tidak berhak" dari "tidak ada
datanya" di matriks ini.

Empat hal yang paling mudah tertukar, dan karena itu diuji eksplisit:

- **401 bukan 403.** Guest belum diketahui identitasnya sehingga dibalas 401;
  akun `tanparole@masesas.test` identitasnya jelas tetapi tidak berhak, sehingga
  dibalas 403. Lihat `role-guest.http` dan `role-tanpa-peran.http`.
- **`@PreAuthorize` method mengalahkan `@PreAuthorize` kelas, bukan
  menambahinya.** `KaryawanController` mengizinkan enam peran di level kelas,
  tetapi `POST` hanya ADMIN dan MANAGER — bukan irisan enam peran dengan dua.
- **Dua peran menghasilkan gabungan hak, bukan irisan.** Lihat
  `role-multi-manager-sales.http`.
- **SUPERADMIN tidak ditulis di satu pun `@PreAuthorize`.** Haknya datang dari
  bean `RoleHierarchy` di `SecurityConfig`: `ROLE_SUPERADMIN` mengimplikasikan
  seluruh peran di `PERAN_DI_BAWAH_SUPERADMIN`. Token dan `AppUser`-nya tetap
  hanya berisi satu peran, `SUPERADMIN` — perluasan terjadi saat pemeriksaan
  otorisasi, bukan saat login. Konsekuensinya: peran baru yang dipakai di
  `@PreAuthorize` harus ikut ditambahkan ke daftar itu, dan
  `RbacSuperadminTest.hierarkiMencakupSeluruhPeranDiAnotasi` gagal bila lupa.

## Baris terakhir dari matriks di atas

Endpoint tanpa `@PreAuthorize` hanya dilindungi `anyRequest().authenticated()`
di `SecurityConfig`. Artinya semua yang punya token bisa masuk — termasuk
CUSTOMER dan akun tanpa peran sama sekali. Itu perilaku yang saat ini memang
berlaku, bukan kegagalan test, dan berkas `role-customer.http` serta
`role-tanpa-peran.http` sengaja mendokumentasikannya.

## Catatan menjalankan

- **Data.** `role-superadmin.http`, `role-admin.http`, `role-manager.http`,
  `role-hr.http`, dan `role-multi-manager-sales.http` membuat data lalu
  menghapusnya kembali di bagian terakhir, jadi aman dijalankan berulang. Berkas peran lain sama sekali
  tidak mengubah data.
- **Stored procedure.** Request ke `/api/sp/karyawan/**` hanya dipastikan
  "bukan 401/403", karena hasil 200-nya bergantung pada stored procedure di
  `stored_procedure_postgresql.sql` sudah terpasang atau belum. Yang diuji di
  sini otorisasinya, bukan prosedurnya.
- **Kuncian login.** Tidak ada rate limit per IP. Yang berlaku hanya kuncian
  per akun dari `LoginAttemptService`, jadi login gagal berulang pada satu email
  membalas 423 sampai kuncian kedaluwarsa.
- **Periode payroll.** Dihitung otomatis di script login sebagai tanggal 1 bulan
  berjalan. `PayrollServiceImpl` menolak periode yang melewati bulan berjalan,
  jadi tanggal ini tidak boleh diganti ke masa depan.

## Rujukan

- Aturan otorisasi yang berlaku: `config/SecurityConfig.java` dan anotasi `@PreAuthorize` di controller
- Peta akses yang dihasilkan aplikasi: `GET /api/rolemap/matriks`
- Rancangan peran dari database: [IMPLEMENTATION-PLAN-RBAC-FASE3.md](../IMPLEMENTATION-PLAN-RBAC-FASE3.md)
