# Implementation Plan — RBAC Fase 1: Model Data

Fase 1 hanya menyiapkan **lapisan data**: tabel `role`, `karyawan_role`, `customer`, penambahan kolom di `karyawan`, entity JPA, dan repository. Belum ada satu pun perubahan pada Spring Security.

Lapisan keamanannya (login, register, JWT, `@PreAuthorize`) ada di [IMPLEMENTATION-PLAN-RBAC-FASE2.md](./IMPLEMENTATION-PLAN-RBAC-FASE2.md).

Pemisahan ini disengaja: fase 1 bisa dijalankan, diseed, dan diverifikasi lewat test repository tanpa menyentuh 90 test OWASP yang sudah hijau.

---

## 1. Keputusan yang sudah disepakati

| Hal | Keputusan | Alasan |
|---|---|---|
| Akun demo lama (`admin`/`hr`/`karyawan`) | Tetap hidup di memori, jadi **fallback** setelah pencarian ke DB gagal | ~90 test OWASP memakainya, termasuk peran `HR` yang tidak ada di daftar peran baru |
| Login karyawan vs customer | **Endpoint terpisah** | Tidak ada ambiguitas kalau email yang sama muncul di dua tabel |
| Relasi karyawan ↔ role | **Entity `KaryawanRole` eksplisit** (bukan `@ManyToMany`) | Entity `Karyawan` dipakai cache Redis dan belasan query lain — menambah koleksi ke sana berisiko; join table punya class sendiri lebih mudah dibaca peserta |
| Primary key `karyawan_role` | **Surrogate `id` identity** + unique `(id_karyawan, id_role)` | Menghindari `@IdClass`/`@EmbeddedId` yang berat untuk materi training |
| Migrasi DB | Dijalankan lewat `psql` ke schema `masesas` | Test di repo ini menembak PostgreSQL asli, bukan H2 |
| Peran karyawan | `ADMIN`, `MANAGER`, `MARKETING`, `SALES` | Sesuai permintaan |
| Peran customer | `CUSTOMER`, di-hardcode di kode | Customer tidak butuh tabel relasi peran — satu peran tetap |

---

## 2. Analisa kolom: apa saja yang benar-benar dibutuhkan

Permintaan awal menyebut `email` dan `password` di `karyawan`. Ini hasil telusuran kolom lain yang diperlukan supaya tujuan (login + RBAC) tercapai, dan yang sengaja **tidak** dipakai.

### Dibutuhkan

| Tabel | Kolom | Kenapa |
|---|---|---|
| `karyawan` | `email varchar(150) NULL UNIQUE` | Identitas login. **Wajib nullable** — `Karyawan2Service` sudah punya `INSERT INTO masesas.karyawan (nama, alamat, dob, status, created_date)` yang tidak mengisi kolom ini; kalau `NOT NULL`, endpoint itu langsung pecah |
| `karyawan` | `password varchar(100) NULL` | Hash bcrypt. Panjang 100 karena format `{bcrypt}$2y$12$...` = 68 karakter, sisanya ruang kalau algoritma diganti. Nullable dengan alasan yang sama seperti email |
| `role` | `id`, `nama varchar(50) UNIQUE` | `nama` unik supaya seed idempoten (`ON CONFLICT (nama) DO NOTHING`) dan supaya tidak ada dua baris `ADMIN` |
| `karyawan_role` | `id`, `id_karyawan`, `id_role`, unique `(id_karyawan, id_role)` | Unik gabungan mencegah peran ganda pada karyawan yang sama |
| `customer` | `id`, `nama`, `email UNIQUE`, `password` | Kebutuhan minimum register + login |

### Sengaja tidak ditambahkan

| Kolom yang biasanya muncul | Kenapa dilewati |
|---|---|
| `karyawan.enabled` / `is_active` | Kolom `status` (`AKTIF`/`NONAKTIF`) sudah ada dan sudah dipakai. Menambah flag kedua berarti dua sumber kebenaran |
| `role.deskripsi` | Tidak dibaca kode mana pun. Nama peran sudah menjelaskan dirinya |
| `customer.role` / tabel `customer_role` | Semua customer berperan sama. Satu peran tetap cukup di-hardcode |
| `password_changed_at`, `last_login`, `failed_attempt` | Penguncian brute force sudah ditangani `LoginAttemptService` lewat Redis (A07) |
| `refresh_token` | Di luar ruang lingkup — token yang ada STATELESS dengan TTL 15 menit |

> **Catatan konsistensi:** `customer` memakai trio `created_date` / `updated_date` / `deleted_date` mengikuti tabel lain di schema `masesas`. `role` dan `karyawan_role` cukup `created_date` — dua tabel ini tabel referensi, tidak pernah di-soft-delete.

---

## 3. Migrasi SQL

File: `rbac_masesas.sql` di root, mengikuti pola `payroll_karyawan_masesas.sql`.

Aman dijalankan berulang: semua DDL memakai `IF NOT EXISTS` dan semua seed memakai `ON CONFLICT DO NOTHING`.

### 3.1 DDL

```sql
CREATE TABLE IF NOT EXISTS masesas.role (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nama         varchar(50) NOT NULL,
    created_date timestamp DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_role_nama UNIQUE (nama)
);

CREATE TABLE IF NOT EXISTS masesas.karyawan_role (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_karyawan  integer NOT NULL,
    id_role      integer NOT NULL,
    created_date timestamp DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_kr_karyawan FOREIGN KEY (id_karyawan) REFERENCES masesas.karyawan (id) ON DELETE CASCADE,
    CONSTRAINT fk_kr_role     FOREIGN KEY (id_role)     REFERENCES masesas.role (id),
    CONSTRAINT uq_karyawan_role UNIQUE (id_karyawan, id_role)
);

CREATE TABLE IF NOT EXISTS masesas.customer (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nama         varchar(100) NOT NULL,
    email        varchar(150) NOT NULL,
    password     varchar(100) NOT NULL,
    created_date timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_date timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted_date timestamp,
    CONSTRAINT uq_customer_email UNIQUE (email)
);

ALTER TABLE masesas.karyawan ADD COLUMN IF NOT EXISTS email    varchar(150);
ALTER TABLE masesas.karyawan ADD COLUMN IF NOT EXISTS password varchar(100);
```

Unique `karyawan.email` ditambahkan lewat blok `DO $$ ... $$` yang memeriksa `pg_constraint` dulu, karena `ADD CONSTRAINT` tidak punya `IF NOT EXISTS`.

`role` bukan kata kunci terpesan (*reserved*) di PostgreSQL, jadi nama tabelnya boleh ditulis tanpa tanda kutip.

### 3.2 Seed peran dan akun demo

Hash password **tidak ditulis di dalam file SQL**. File hanya memakai variabel psql `:'pwd_hash'`, nilainya dipasok saat eksekusi — supaya tidak ada kredensial baru yang ikut ter-commit (aturan Keamanan di `CLAUDE.md`).

```bash
HASH="{bcrypt}$(htpasswd -bnBC 12 "" "$DEMO_PASSWORD" | tr -d ':\n')"
psql -h <host> -p 5432 -U <user> -d binar_finance -v pwd_hash="$HASH" -f rbac_masesas.sql
```

Akun yang diseed — semua memakai password yang sama, yaitu `DEMO_PASSWORD` dari `.env`:

| id karyawan | email | Peran | Dipakai untuk mendemokan |
|---|---|---|---|
| 1 | `admin@masesas.test` | ADMIN | Akses penuh |
| 2 | `manager@masesas.test` | MANAGER | Akses menengah |
| 3 | `marketing@masesas.test` | MARKETING | Akses terbatas |
| 4 | `sales@masesas.test` | SALES | Akses terbatas |
| 5 | `manager.sales@masesas.test` | MANAGER + SALES | Satu karyawan banyak peran |
| 6 | `tanparole@masesas.test` | — | **Login berhasil tapi ditolak 403** — beda antara *authenticated* dan *authorized* |

Karyawan 7–1000 hanya diisi `email` berpola `karyawanNNNN@masesas.test` dengan `password` tetap `NULL`, jadi tidak bisa login sama sekali. Ini disengaja: kolom email terisi konsisten untuk latihan query, tanpa membuka 994 akun yang bisa dipakai masuk.

Customer seed: `customer1@masesas.test` dan `customer2@masesas.test`, password sama.

---

## 4. Perubahan kode Java

Semua tipe baru ditulis sebagai **class biasa** dengan Lombok, bukan `record` (aturan wajib #1 `CLAUDE.md`). Tidak ada komentar di file `.java`.

### File baru

| File | Isi |
|---|---|
| `entity/Role.java` | `id`, `nama`, `createdDate`. `@Table(name = "role", schema = "masesas")` |
| `entity/KaryawanRole.java` | `id`, `@ManyToOne(LAZY) Karyawan karyawan` → `id_karyawan`, `@ManyToOne(LAZY) Role role` → `id_role`, `createdDate` |
| `entity/Customer.java` | `id`, `nama`, `email`, `password`, `createdDate`, `updatedDate`, `deletedDate` |
| `repository/KaryawanRoleRepository.java` | `List<KaryawanRole> findAllByKaryawan_Id(Integer idKaryawan)`, memakai `@EntityGraph("role")` |
| `repository/CustomerRepository.java` | `Optional<Customer> findByEmailAndDeletedDateIsNull(String email)`, `boolean existsByEmail(String email)` |

`RoleRepository` **tidak dibuat**: tidak ada satu pun alur yang perlu membaca tabel `role` secara langsung. Peran selalu diambil lewat `KaryawanRole`.

### File yang diubah

| File | Perubahan |
|---|---|
| `entity/Karyawan.java` | Tambah dua field `String email` dan `String password` |
| `repository/KaryawanRepository.java` | Tambah `Optional<Karyawan> findByEmailAndDeletedDateIsNull(String email)` dan `boolean existsByEmail(String email)` |

`@ManyToOne` di `KaryawanRole` dibuat `LAZY` supaya memuat daftar peran tidak ikut menarik seluruh baris karyawan. Sisi `role` ditarik sekaligus lewat `@EntityGraph(attributePaths = "role")` di repository — satu anotasi yang menghapus N+1 saat membaca `kr.getRole().getNama()`, tanpa memaksa relasi jadi `EAGER` untuk semua pemakaian lain.

---

## 5. Dampak ke kode yang sudah ada

Ditelusuri satu per satu sebelum eksekusi:

| Titik | Dampak | Penanganan |
|---|---|---|
| `Karyawan2Service` — `INSERT` tanpa email/password | Pecah kalau kolom `NOT NULL` | Kolom dibuat nullable |
| `KaryawanResponse` | Aman — daftar fieldnya eksplisit, tidak ada `password` yang bocor | Diverifikasi ulang setelah entity diubah |
| `A03SqlSafeController` / `A03SqlVulnController` | Aman — `SELECT id, nama, alamat, status`, bukan `SELECT *` | — |
| Cache Redis `KaryawanServiceImpl` | Yang di-cache `KaryawanResponse`, bukan entity | — |
| Stored procedure `sp_*_karyawan_*` | Tidak menyentuh kolom baru | — |
| Enkripsi NIK/NPWP (A02) | Ada di `detail_karyawan`, tabel berbeda | — |

**Aturan pengaman:** `password` tidak boleh muncul di DTO response mana pun. Diverifikasi manual setelah entity diubah, dan dijaga test di fase 2.

---

## 6. Test fase 1

File baru `src/test/java/com/masesas/exercises/demo1/rbac/RbacSkemaTest.java` (`@SpringBootTest`):

1. `karyawanRepository.findByEmailAndDeletedDateIsNull("admin@masesas.test")` mengembalikan karyawan id 1
2. `karyawanRoleRepository.findAllByKaryawan_Id(1)` menghasilkan tepat satu peran, namanya `ADMIN`
3. `findAllByKaryawan_Id(5)` menghasilkan dua peran: `MANAGER` dan `SALES`
4. `findAllByKaryawan_Id(6)` menghasilkan daftar kosong
5. `passwordEncoder.matches(demoPassword, karyawan.getPassword())` bernilai true — membuktikan hash hasil `htpasswd` benar-benar dikenali `DelegatingPasswordEncoder` aplikasi
6. `customerRepository.findByEmailAndDeletedDateIsNull("customer1@masesas.test")` ditemukan

Poin 5 yang paling penting: kalau format hash tidak cocok, kegagalannya baru ketahuan saat login di fase 2 dan sulit dilacak.

---

## 7. Checklist eksekusi

- [x] Tulis `rbac_masesas.sql` (DDL + seed, memakai `:'pwd_hash'`)
- [x] Jalankan migrasi lewat `psql` — 4 peran, 6 pemberian peran, 2 customer, 6 karyawan berpassword, 1000 karyawan beremail
- [x] Tambah `email` + `password` di `entity/Karyawan.java`
- [x] Buat `Role`, `KaryawanRole`, `Customer`
- [x] Buat `KaryawanRoleRepository`, `CustomerRepository`, tambah dua method di `KaryawanRepository`
- [x] `./mvnw clean compile` hijau
- [x] `./mvnw test` — 90/90 test lama tetap hijau
- [x] Periksa ulang tidak ada response DTO yang membawa `password` — `KaryawanResponse` berfield eksplisit, tidak ada controller yang mengembalikan entity `Karyawan` langsung
- [x] Verifikasi hash tersimpan utuh (68 karakter, awalan `{bcrypt}$2y`) dan cocok dengan `DEMO_PASSWORD`
- [ ] `RbacSkemaTest` — **ditunda atas permintaan**, digabung ke rangkaian test fase 2
- [ ] Commit `feat: RBAC fase 1 model data role, karyawan_role, customer`

---

## 8. Batasan explicit

**Di luar ruang lingkup fase 1:**
- Login, register, JWT, `@PreAuthorize` — semuanya fase 2
- Endpoint CRUD untuk mengelola peran (assign/revoke role lewat API). Peran diatur lewat SQL
- Hirarki peran (`RoleHierarchy` Spring Security) — daftar peran ini datar, tidak ada yang mewarisi yang lain

**Keterbatasan yang diterima:**
- Keunikan email **tidak** dijamin lintas tabel `karyawan` dan `customer`; masing-masing hanya unik di tabelnya sendiri. Penjagaannya di lapisan aplikasi saat register (fase 2)
- Migrasi dijalankan manual, bukan lewat Flyway/Liquibase — mengikuti kebiasaan repo ini yang menyimpan `.sql` lepas di root
- Semua akun demo memakai satu password yang sama. Cukup untuk training, jelas tidak untuk lingkungan nyata
