# Implementation Plan — Permission Berbutir Halus di Atas RBAC

Menambahkan izin spesifik (`KARYAWAN_CREATE`, `PAYROLL_APPROVE`, dan seterusnya) di
atas RBAC yang sudah berjalan, tanpa membuang satu pun aturan `hasAnyRole` yang ada
sekarang.

---

## 1. Dasar: Spring Security tidak membedakan role dan permission

Keduanya adalah objek yang sama — `GrantedAuthority`, sebuah string. `hasRole('ADMIN')`
dievaluasi secara harfiah menjadi `hasAuthority('ROLE_ADMIN')`; satu-satunya perbedaan
adalah awalan `ROLE_` yang ditambahkan otomatis.

Konsekuensinya: permission berbutir halus tidak butuh mekanisme baru. Cukup authority
**tanpa awalan `ROLE_`**, dijaga dengan `hasAuthority(...)`.

```java
@PreAuthorize("hasAuthority('KARYAWAN_CREATE')")
```

Tidak ada `PermissionEvaluator`, tidak ada modul Spring Security ACL, tidak ada
konfigurasi tambahan. `@EnableMethodSecurity` di `SecurityConfig:27` sudah cukup.

## 2. Prinsip yang mengikat seluruh rencana ini

**Migrasi ini setara-perilaku (behavior-preserving).** Setelah selesai, matriks
akses yang berlaku harus persis sama dengan hari ini — siapa pun yang bisa mengakses
sebuah endpoint sekarang, masih bisa; yang tidak bisa, tetap tidak bisa.

Alasannya: pemisahan izin dan pengetatan izin adalah dua perubahan berbeda. Kalau
digabung, saat ada test yang merah kita tidak tahu apakah itu bug migrasi atau
konsekuensi pengetatan yang memang diinginkan. Pengetatan (misal "MANAGER tidak
boleh lagi approve payroll") dikerjakan setelah ini, sebagai keputusan terpisah.

Bukti prinsip ini: `RbacKaryawanTest`, `RbacCustomerTest`, dan `RbacGuestTest` harus
lulus **tanpa satu baris pun diubah**.

## 3. Aturan penulisan ekspresi lama

Sesuai permintaan: **ekspresi `hasAnyRole`/`hasRole` yang ada sekarang tidak dihapus.**
Ekspresi lama dijadikan komentar tepat di atas penggantinya, dengan catatan tetap.

```java
// RBAC murni — dipakai sebelum tabel permission ada. Simpan sebagai rujukan
// dan jalur rollback; jangan diaktifkan bersamaan dengan @PreAuthorize di bawah.
// @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@PreAuthorize("hasAuthority('KARYAWAN_CREATE')")
```

Kalimat catatan dipakai seragam di seluruh berkas, sehingga bisa dicari dengan
`grep -rn "RBAC murni"`.

> **Catatan konflik aturan.** `CLAUDE.md` Aturan Wajib no. 3 melarang komentar apa pun
> di berkas `.java`. Instruksi menyimpan ekspresi lama sebagai komentar melanggar
> aturan itu secara langsung. Dua opsi penyelesaian, keduanya sah — pilih satu sebelum
> eksekusi dimulai:
>
> | Opsi | Konsekuensi |
> |---|---|
> | **A.** Tambahkan pengecualian tertulis di `CLAUDE.md` no. 3: komentar `@PreAuthorize` versi RBAC murni diizinkan. | Aturan tetap utuh dan jujur; ada satu pengecualian yang bisa diaudit. |
> | **B.** Ekspresi lama tidak ditulis di `.java`, melainkan disimpan lengkap di tabel §5 dokumen ini. | `CLAUDE.md` tidak perlu diubah, tapi rujukan tidak lagi bersebelahan dengan kodenya. |
>
> Rencana ini ditulis dengan asumsi **Opsi A** (sesuai instruksi). Kalau Opsi B yang
> dipilih, langkah §7.6 dilewati dan tabel §5 menjadi satu-satunya sumber.

Alasan menyimpannya sama sekali: dua sistem otorisasi berjalan berdampingan dalam
transisi ini (lihat §4), dan ekspresi lama adalah jalur rollback yang paling murah —
menukar dua baris komentar, bukan menulis ulang.

## 4. Batas dua model: karyawan vs customer

| Aktor | Model otorisasi | Ekspresi |
|---|---|---|
| Karyawan (internal) | Permission | `hasAuthority('KARYAWAN_CREATE')` |
| Customer (eksternal) | Role murni | `hasRole('CUSTOMER')` |
| Guest (anonim) | Role sintetis | tidak dijaga; hanya endpoint publik |

### Kenapa customer sengaja tidak diberi permission

Ini keputusan yang disengaja, bukan bagian yang belum dikerjakan.

1. **CUSTOMER bukan baris di tabel `role`.** Nilainya di-hardcode di
   `AppUserDetailsService:48` (`List.of(ROLE_CUSTOMER)`), tidak berasal dari database.
   Memberi permission ke customer berarti membuat sumber ketiga (`customer_permission`)
   hanya untuk melayani satu endpoint.
2. **Permukaan aksesnya satu endpoint dan self-scoped.** `/api/customer/me` hanya
   membaca data diri sendiri. Granularitas tidak membeli apa pun kalau cuma ada satu
   aksi atas data sendiri.
3. **Otomatis fail-closed.** Setelah endpoint internal dijaga `hasAuthority(...)` dan
   customer tidak memegang permission apa pun, customer tertutup dari seluruh endpoint
   karyawan dan payroll tanpa aturan tambahan. Ini lebih aman daripada daftar role
   panjang yang harus selalu diingat untuk tidak menyertakan CUSTOMER.

### Syarat yang menyertai keputusan itu

- **Batasnya ditulis dan dipatuhi**, tidak setengah-setengah: internal = permission,
  eksternal = role. Tidak ada endpoint customer yang dijaga `hasAuthority`.
- **Pintu migrasi tidak dikunci.** Field `permissions` di `AppUser` tetap ada untuk
  customer, diisi `List.of()` di satu tempat (`AppUserDetailsService.findCustomer`).
  Tidak boleh ada cabang `if (tipe == CUSTOMER)` yang tersebar di kode otorisasi.
- **Pemicu peninjauan ulang, ditulis eksplisit:** begitu customer butuh pembedaan hak
  akses (contoh: tingkatan `CUSTOMER_PREMIUM`, atau endpoint customer melebihi tiga
  dengan hak berbeda-beda), pindahkan customer ke permission. Tanpa pemicu tertulis,
  pola yang biasanya terjadi adalah penambahan role satu per satu
  (`CUSTOMER_PREMIUM`, `CUSTOMER_TRIAL`, `CUSTOMER_LAMA`) — persis masalah yang
  ingin dihindari permission.

### Risiko yang harus ditangani karena dua model ini

`/api/rolemap` akan mencampur dua model dalam satu respons: baris CUSTOMER bersumber
dari `roles`, baris ADMIN dari `permissions`. Kalau DTO dan penyaringnya hanya
mengenal satu, salah satu baris akan melaporkan "0 endpoint" — bug tampilan yang
menyamar sebagai temuan keamanan. Ditangani di §7.7.

---

## 5. Katalog permission

Diturunkan langsung dari `@PreAuthorize` yang ada sekarang, sehingga kolom terakhir
adalah matriks yang berlaku hari ini.

**Catatan penting soal warisan anotasi:** di Spring Security, `@PreAuthorize` pada
method **menggantikan sepenuhnya** yang ada di class — bukan digabung dengan AND.
Jadi `POST /api/karyawan` hari ini hanya menuntut `ADMIN` atau `MANAGER`; daftar enam
role di level class tidak ikut dievaluasi. Karena itu setiap permission di bawah
berdiri sendiri, bukan `KARYAWAN_READ and KARYAWAN_CREATE`. Perilaku ini diverifikasi
oleh test di §8.1, bukan diasumsikan.

### KaryawanController

| Permission | Endpoint | Ekspresi RBAC murni (disimpan sebagai komentar) | Role pemegang |
|---|---|---|---|
| `KARYAWAN_READ` | level class — seluruh GET + `POST /{id}/upload` | `hasAnyRole('ADMIN','MANAGER','MARKETING','SALES','HR','KARYAWAN')` | ADMIN, MANAGER, MARKETING, SALES, HR, KARYAWAN |
| `KARYAWAN_CREATE` | `POST /api/karyawan` | `hasAnyRole('ADMIN','MANAGER')` | ADMIN, MANAGER |
| `KARYAWAN_UPDATE` | `PUT /api/karyawan/{id}` | `hasAnyRole('ADMIN','MANAGER')` | ADMIN, MANAGER |
| `KARYAWAN_DELETE` | `DELETE /api/karyawan/{id}` | `hasRole('ADMIN')` | ADMIN |
| `KARYAWAN_DETAIL_UPDATE` | `PUT /api/karyawan/{id}/detail` | `hasAnyRole('ADMIN','MANAGER')` | ADMIN, MANAGER |
| `KARYAWAN_DETAIL_DELETE` | `DELETE /api/karyawan/{id}/detail` | `hasAnyRole('ADMIN','MANAGER')` | ADMIN, MANAGER |

### PayrollController

| Permission | Endpoint | Ekspresi RBAC murni (disimpan sebagai komentar) | Role pemegang |
|---|---|---|---|
| `PAYROLL_READ` | level class — seluruh GET | `hasAnyRole('ADMIN','MANAGER','HR')` | ADMIN, MANAGER, HR |
| `PAYROLL_CREATE` | `POST /api/payroll` | (mewarisi level class) | ADMIN, MANAGER, HR |
| `PAYROLL_UPDATE` | `PUT /api/payroll/{idKaryawan}/{periode}` | (mewarisi level class) | ADMIN, MANAGER, HR |
| `PAYROLL_APPROVE` | `POST /api/payroll/{idKaryawan}/{periode}/approve` | (mewarisi level class) | ADMIN, MANAGER, HR |
| `PAYROLL_DELETE` | `DELETE /api/payroll/{idKaryawan}/{periode}` | `hasAnyRole('ADMIN','HR')` | ADMIN, HR |

Empat baris payroll pertama hari ini dijaga oleh satu ekspresi yang sama. Memecahnya
menjadi empat permission adalah inti dari fitur ini — tetapi seed di §6 memberikan
keempatnya ke ADMIN, MANAGER, dan HR, sehingga perilakunya belum berubah. Pengetatan
menyusul sebagai keputusan terpisah (§10).

### Tidak berubah

| Endpoint | Ekspresi | Alasan |
|---|---|---|
| `GET /api/customer/me` | `hasRole('CUSTOMER')` | Role murni sesuai §4 |
| `/api/rolemap/**` | tanpa anotasi | Publik lewat `SecurityConfig.PATH_PUBLIK` |
| `/api/auth/**` | tanpa anotasi | Publik lewat `SecurityConfig.PATH_PUBLIK` |

---

## 6. Perubahan database

Skema `masesas`, mengikuti bentuk `karyawan_role` yang sudah ada (surrogate `id`,
kolom FK berawalan `id_`, `created_date`).

```sql
CREATE TABLE IF NOT EXISTS masesas.permission (
    id           serial PRIMARY KEY,
    nama         varchar(64) NOT NULL UNIQUE,
    created_date timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS masesas.role_permission (
    id            serial PRIMARY KEY,
    id_role       integer NOT NULL REFERENCES masesas.role(id),
    id_permission integer NOT NULL REFERENCES masesas.permission(id),
    created_date  timestamptz NOT NULL DEFAULT now(),
    UNIQUE (id_role, id_permission)
);
```

`UNIQUE (id_role, id_permission)` bukan hiasan — tanpa itu `ON CONFLICT DO NOTHING`
pada seed tidak punya sasaran, dan menjalankan berkas dua kali akan menggandakan
baris.

Tabel `role` dan `karyawan_role` **tidak disentuh**. Role tetap menjadi wadah;
yang berubah hanya isinya sekarang bisa dirinci.

### Berkas seed

Berkas baru di root: `permission_masesas.sql`, mengikuti pola
`rbac_role_penuh_masesas.sql` — satu transaksi (`BEGIN`/`COMMIT`), aman dijalankan
berulang, tanpa variabel `-v`.

Isinya tiga bagian:
1. DDL kedua tabel di atas.
2. `INSERT` sebelas nama permission dari §5, `ON CONFLICT (nama) DO NOTHING`.
3. `INSERT` pemetaan `role_permission` sesuai kolom "Role pemegang" di §5, dengan
   `SELECT` id berdasarkan nama (bukan id literal), `ON CONFLICT DO NOTHING`.

Pengaman yang wajib ada, meniru pola `RAISE EXCEPTION` di
`rbac_role_penuh_masesas.sql`: bila ada nama role di §5 yang tidak ditemukan di tabel
`role`, berkas berhenti dengan pesan jelas. Tanpa itu, `INSERT ... SELECT` yang tidak
menemukan role akan memasukkan nol baris tanpa suara — dan hasilnya adalah role yang
kehilangan seluruh aksesnya di produksi.

Urutan jalan: `rbac_role_penuh_masesas.sql` lebih dulu (ia yang membuat role `HR` dan
`KARYAWAN`), baru `permission_masesas.sql`.

---

## 7. Perubahan kode

### 7.1 `entity/Permission.java` (baru)

Salinan bentuk `entity/Role.java`: `id`, `nama`, `createdDate`, `@Table(name = "permission", schema = "masesas")`.

### 7.2 `entity/RolePermission.java` (baru)

Salinan bentuk `entity/KaryawanRole.java`: `id`, `@ManyToOne(LAZY) Role role`
(`@JoinColumn(name = "id_role")`), `@ManyToOne(LAZY) Permission permission`
(`@JoinColumn(name = "id_permission")`), `createdDate`.

### 7.3 `repository/RolePermissionRepository.java` (baru)

```java
public interface RolePermissionRepository extends JpaRepository<RolePermission, Integer> {

    @EntityGraph(attributePaths = {"role", "permission"})
    List<RolePermission> findAllByRole_IdIn(Collection<Integer> idRole);

    @Override
    @EntityGraph(attributePaths = {"role", "permission"})
    List<RolePermission> findAll();
}
```

`findAllByRole_IdIn` dipakai saat login; `findAll` yang di-override dipakai
`RoleMapService`. Keduanya butuh `@EntityGraph` — relasinya `LAZY`, dan tanpa
`@EntityGraph` pembacaan `getPermission().getNama()` di luar transaksi akan melempar
`LazyInitializationException`, bukan sekadar memicu N+1.

### 7.4 `security/AppUser.java`

Tambah field `private final List<String> permissions;` setelah `roles`, lalu:

```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return Stream.of(
                    roles.stream().map(role -> "ROLE_" + role),
                    permissions.stream(),
                    Stream.of(ROLE_GUEST))
            .flatMap(Function.identity())
            .map(SimpleGrantedAuthority::new)
            .toList();
}
```

Permission masuk **tanpa** awalan — itulah yang membuat `hasAuthority('KARYAWAN_CREATE')`
cocok sementara `hasRole('ADMIN')` tetap cocok lewat `ROLE_ADMIN`.

Field baru mengubah tanda tangan `@AllArgsConstructor`. Tiga pemanggil harus ikut
disesuaikan — bukan dua:

- `AppUserDetailsService:45` (customer) → sisipkan `List.of()`
- `AppUserDetailsService:57` (karyawan) → sisipkan daftar hasil §7.5
- `RbacKaryawanTest:141` → sisipkan `List.of()`

Pemanggil ketiga ada di test dan tidak akan terlihat oleh `./mvnw compile`; hanya
`./mvnw test` yang menangkapnya.

### 7.5 `security/AppUserDetailsService.java`

Injeksi `RolePermissionRepository`, lalu:

```java
private AppUser toAppUser(Karyawan karyawan) {
    List<KaryawanRole> karyawanRoles = karyawanRoleRepository.findAllByKaryawan_Id(karyawan.getId());
    List<String> roles = karyawanRoles.stream()
            .map(karyawanRole -> karyawanRole.getRole().getNama())
            .toList();
    List<Integer> idRole = karyawanRoles.stream()
            .map(karyawanRole -> karyawanRole.getRole().getId())
            .toList();
    List<String> permissions = idRole.isEmpty()
            ? List.of()
            : rolePermissionRepository.findAllByRole_IdIn(idRole).stream()
                    .map(rolePermission -> rolePermission.getPermission().getNama())
                    .distinct()
                    .toList();
    return new AppUser(
            karyawan.getEmail(), karyawan.getPassword(), roles, permissions,
            karyawan.getId(), AppUser.TIPE_KARYAWAN);
}
```

Dua hal yang tidak boleh dilewat:

- **Guard `idRole.isEmpty()`.** Akun `tanparole@masesas.test` (karyawan id 6) sengaja
  dipertahankan tanpa role oleh `RbacKaryawanTest`. Tanpa guard, query berubah menjadi
  `IN ()` yang ditolak PostgreSQL — login yang seharusnya berhasil-lalu-403 malah
  menjadi 500.
- **`.distinct()`.** Karyawan bisa memegang lebih dari satu role (`manager.sales@masesas.test`
  memegang MANAGER dan SALES). Tanpa `distinct`, permission yang dimiliki kedua role
  muncul ganda di daftar authority. Tidak salah secara fungsi, tapi mengotori respons
  `/api/rolemap` dan pesan debug.

`findCustomer` (baris 43–51) hanya menyisipkan `List.of()` sebagai argumen permission.
Tidak ada perubahan lain — ini satu-satunya tempat yang menyatakan "customer tidak
punya permission", sesuai syarat di §4.

### 7.6 Controller

`KaryawanController` (6 titik) dan `PayrollController` (2 titik yang sudah ada +
3 titik baru untuk memecah `PAYROLL_CREATE`/`UPDATE`/`APPROVE` dari level class).

Bentuk yang dipakai, seragam:

```java
// RBAC murni — dipakai sebelum tabel permission ada. Simpan sebagai rujukan
// dan jalur rollback; jangan diaktifkan bersamaan dengan @PreAuthorize di bawah.
// @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@PreAuthorize("hasAuthority('KARYAWAN_UPDATE')")
```

`CustomerController` tidak disentuh sama sekali.

### 7.7 `RoleMapService` dan DTO — bagian yang paling mudah salah

Kalau langkah ini dilewat, `/api/rolemap` tidak sekadar kurang lengkap; ia melaporkan
matriks akses yang **salah**. Tiga cacat yang akan muncul kalau `RoleMapService`
dibiarkan apa adanya:

1. `PEMANGGILAN_PERAN` (`RoleMapService:36`) hanya mengenali `has(Any)Role`. Endpoint
   berpenjaga `hasAuthority` menghasilkan daftar role kosong.
2. `kondisional()` (baris 130–133) membuang pemanggilan role dari ekspresi lalu
   memeriksa sisanya. Karena `hasAuthority('X')` tidak ikut terbuang, sisanya tidak
   kosong dan endpoint ditandai `conditional = true`.
3. `bisaDiakses` baris 149 (`|| endpoint.isConditional()`) kemudian meloloskan endpoint
   itu untuk **setiap** role. Hasil akhirnya: `/api/rolemap` melaporkan MARKETING boleh
   menghapus karyawan. Salah, dan salah ke arah yang berbahaya.

Perubahan yang diperlukan:

- **Pola baru** `PEMANGGILAN_PERMISSION = Pattern.compile("has(?:Any)?Authority\\(([^)]*)\\)")`,
  diurai dengan `NAMA_PERAN` yang sudah ada.
- **`kondisional()`** membuang kedua pola sebelum memeriksa sisa.
- **`EndpointAksesResponse`** mendapat field `List<String> permissions`. Nama field
  mengikuti gaya berkas saat ini yang berbahasa Inggris (`method`, `path`, `handler`,
  `isPublic`, `roles`, `conditional`, `expressions`).
- **`RoleAksesResponse`** mendapat field `List<String> permissions` — daftar permission
  yang dipegang role tersebut. Inilah yang menjawab kebutuhan awal: "role X sebenarnya
  boleh apa saja".
- **`bisaDiakses(endpoint, peran)`** menjadi: publik → true; GUEST → false; tanpa
  ekspresi → true; selain itu true bila `endpoint.getRoles().contains(peran)` **atau**
  permission milik `peran` beririsan dengan `endpoint.getPermissions()` **atau**
  `endpoint.isConditional()`.
- **Sumber permission per role** diambil dari `rolePermissionRepository.findAll()`,
  dikelompokkan menjadi `Map<String, Set<String>>` (nama role → nama permission) satu
  kali per pemanggilan `semuaPeran()`, bukan satu query per role.

Cabang `endpoint.getRoles().contains(peran)` tetap dipertahankan, bukan sisa yang lupa
dibuang: itulah yang membuat baris CUSTOMER (§4) tetap terisi.

> **Peringatan koordinasi.** `RoleMapService`, `EndpointAksesResponse`, dan
> `RoleAksesResponse` belum masuk git (berkas baru) dan sedang disunting oleh sesi lain —
> nama fieldnya berubah dari Indonesia ke Inggris saat rencana ini disusun. Sebelum
> mengeksekusi §7.7, baca ulang ketiga berkas itu dan sesuaikan nama getter yang
> dirujuk di sini.

---

## 8. Test

Semuanya di `src/test/java/com/masesas/exercises/demo1/`, mengikuti package kode yang
diuji.

### 8.1 `rbac/PermissionAuthorityTest` (baru)

| # | Yang diuji | Menyatakan bahwa |
|---|---|---|
| 1 | `getAuthorities()` untuk ADMIN | memuat `ROLE_ADMIN` **dan** `KARYAWAN_CREATE` sekaligus |
| 2 | Karyawan multi-role (`manager.sales@`) | permission gabungan, tanpa duplikat |
| 3 | Karyawan tanpa role (`tanparole@`) | `permissions` kosong, tidak melempar exception (guard §7.5) |
| 4 | Customer | `permissions` kosong, `ROLE_CUSTOMER` tetap ada |
| 5 | `@PreAuthorize` method menimpa class | `POST /api/karyawan` oleh MARKETING → 403, walau MARKETING ada di daftar level class |

Nomor 5 membuktikan asumsi warisan anotasi di §5 alih-alih memercayainya.

### 8.2 `rbac/RbacPermissionMatrixTest` (baru)

Matriks penuh sebelas permission × role pemegangnya dari §5, sebagai
`@ParameterizedTest` — tiap baris menegaskan role yang berhak mendapat 2xx dan role
yang tidak berhak mendapat 403.

### 8.3 `controller/RoleMapPermissionTest` (baru)

| # | Yang diuji |
|---|---|
| 1 | Endpoint berpenjaga `hasAuthority` **tidak** dilaporkan `conditional` (cacat no. 2 di §7.7) |
| 2 | MARKETING tidak muncul sebagai pemegang akses `DELETE /api/karyawan/{id}` (cacat no. 3) |
| 3 | Baris CUSTOMER tetap berisi `GET /api/customer/me` — bukan nol endpoint (risiko §4) |
| 4 | `RoleAksesResponse.permissions` untuk ADMIN memuat kesebelas permission |

### 8.4 Regresi yang harus lulus tanpa diubah

`RbacKaryawanTest`, `RbacCustomerTest`, `RbacGuestTest`, `RbacRoleDatabaseTest`,
`SecurityWhitelistTest`, `RoleMapControllerTest`.

Satu-satunya suntingan yang dibolehkan di berkas-berkas itu adalah argumen konstruktor
`AppUser` di `RbacKaryawanTest:141` (§7.4) — perubahan tanda tangan, bukan perubahan
ekspektasi. Kalau ada berkas lain yang perlu disunting, itu tanda migrasinya **tidak**
setara-perilaku dan penyebabnya harus dicari, bukan testnya yang disesuaikan.

---

## 9. Urutan eksekusi

| # | Langkah | Gerbang |
|---|---|---|
| 1 | Putuskan Opsi A atau B pada konflik `CLAUDE.md` (§3) | keputusan tertulis |
| 2 | Baca ulang `RoleMapService` + 2 DTO (peringatan §7.7) | nama getter terkonfirmasi |
| 3 | Tulis `permission_masesas.sql`, jalankan ke DB | kedua tabel ada, 11 permission, pemetaan sesuai §5 |
| 4 | Entity + repository (§7.1–7.3) | `./mvnw compile` hijau |
| 5 | `AppUser` + `AppUserDetailsService` (§7.4–7.5) | `./mvnw compile` hijau; 3 pemanggil disesuaikan |
| 6 | Test §8.1 lebih dulu, jalankan — **harus merah** | merah karena alasan yang benar |
| 7 | Controller (§7.6) | §8.1 hijau; regresi §8.4 hijau |
| 8 | Test §8.2, jalankan | hijau |
| 9 | `RoleMapService` + DTO (§7.7) | — |
| 10 | Test §8.3, jalankan | hijau |
| 11 | `./mvnw test` penuh | seluruh suite hijau |
| 12 | Verifikasi artefak, bukan status: panggil `GET /api/rolemap` pada aplikasi hidup, bandingkan baris ADMIN/MARKETING/CUSTOMER dengan §5 | cocok |

Langkah 6 adalah gerbang RED pada TDD: kalau test permission sudah hijau sebelum
controller disentuh, berarti testnya tidak menguji apa yang dikira.

Langkah 12 sengaja dipisah dari langkah 11. Suite hijau membuktikan kode berperilaku
sesuai test; ia tidak membuktikan seed SQL di database benar-benar terpasang.

---

## 10. Batasan explicit

### Di luar lingkup (sengaja tidak dikerjakan)

- **Pengetatan akses.** Memecah `PAYROLL_APPROVE` dari `PAYROLL_READ` membuat
  pengetatan *mungkin*; rencana ini tidak melakukannya (§2). Mencabut
  `PAYROLL_APPROVE` dari MANAGER adalah satu baris `DELETE` di `role_permission`
  setelah ini — keputusan bisnis, bukan keputusan teknis.
- **Otorisasi tingkat baris.** "MANAGER hanya boleh approve payroll divisinya sendiri"
  tidak tercakup. Permission adalah string global; ia tidak tahu soal objek. Kalau
  nanti dibutuhkan, jalur termurah adalah SpEL langsung ke principal —
  `@PreAuthorize("hasAuthority('PAYROLL_APPROVE') and #idKaryawan == principal.idKaryawan")` —
  karena `AppUser` sudah memegang `idKaryawan`. `PermissionEvaluator` custom atau
  modul Spring Security ACL adalah jalur berikutnya, dan keduanya jauh lebih berat.
- **CRUD permission lewat API.** Pengelolaan permission dilakukan lewat SQL. Tidak ada
  endpoint admin, tidak ada UI.
- **Permission untuk customer.** Keputusan §4, dengan pemicu peninjauan ulang yang
  sudah ditulis di sana.

### Edge case yang diketahui dan diterima

- **`ROLE_GUEST` melekat pada setiap pengguna**, termasuk ADMIN dan customer
  (`AppUser:30`). Perilaku lama, tidak diubah rencana ini. Akibatnya
  `hasRole('GUEST')` cocok untuk siapa pun yang sudah login — jangan dipakai sebagai
  penjaga.
- **`@PreAuthorize` method menimpa class, bukan menggabung.** Ditegaskan di §5 dan
  diikat oleh test §8.1 nomor 5.
- **Nama permission peka huruf besar-kecil.** `hasAuthority` tidak menambah awalan dan
  tidak menormalkan huruf. `KARYAWAN_CREATE` ≠ `karyawan_create`. Tidak ada
  perlindungan runtime untuk salah ketik; yang menangkapnya adalah test §8.2.
- **Salah ketik nama permission gagal secara diam-diam ke arah aman.**
  `hasAuthority('KARYAWAN_CRAETE')` tidak akan cocok dengan siapa pun — endpoint
  menjadi tertutup total, bukan terbuka. Ini arah kegagalan yang benar, tapi tetap
  perlu test untuk menemukannya.

### Keterbatasan teknis yang diterima di versi ini

- **Satu query tambahan per request.** `JwtAuthFilter:41` memuat ulang pengguna dari
  database setiap request; sekarang bertambah satu query permission. Keuntungannya:
  pencabutan permission berlaku seketika tanpa perlu login ulang, dan tidak ada
  authority yang tersimpan di dalam token. Cache untuk ini di luar lingkup.
- **`RoleMapService` mengurai SpEL dengan regex, bukan parser.** Ekspresi kompleks
  (`... and #id == principal.idKaryawan`) tetap dilaporkan `conditional = true`, yang
  artinya "tidak bisa disimpulkan otomatis — periksa manual". Batasan yang diterima;
  parser SpEL penuh tidak sebanding untuk endpoint diagnostik.

---

## 11. Rollback

1. `DROP TABLE masesas.role_permission; DROP TABLE masesas.permission;`
2. Di setiap controller, tukar dua baris: aktifkan kembali `@PreAuthorize` yang
   dikomentari, komentari yang `hasAuthority`.
3. Kembalikan `AppUser`, `AppUserDetailsService`, `RoleMapService`, dan kedua DTO.

Langkah 2 adalah alasan utama ekspresi lama disimpan (§3): rollback otorisasi menjadi
suntingan mekanis yang bisa diverifikasi mata, bukan penulisan ulang dari ingatan.
