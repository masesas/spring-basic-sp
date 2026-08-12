# CLAUDE.md

Acuan standar dan aturan kerja untuk proyek `demo1`. Berlaku untuk semua kode baru di repositori ini.

---

## Aturan Wajib

Tiga aturan berikut tidak boleh dilanggar.

### 1. Tidak ada `record` di repositori ini

Semua tipe (DTO, model, value object) ditulis sebagai **class biasa**, bukan `record`. Tidak ada lagi `record` yang tersisa — semuanya sudah dikonversi. Jangan membuat yang baru, jangan mengembalikan yang lama.

- Semua accessor memakai getter biasa (`request.getUsername()`). Tidak ada lagi accessor gaya record di manapun.
- Ada dua pola yang dipakai, pilih sesuai kebutuhannya:

**Pola A — DTO & model (default).** Dipakai kalau objeknya melewati Jackson: `@RequestBody`, atau disimpan sebagai nilai cache Redis. Jackson butuh constructor kosong + setter untuk membangun ulang objeknya, dan `@Data` sekaligus menjaga `equals`/`hashCode` yang dulu gratis dari `record`.

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RekeningResponse {
    private String nomor;
    private String bank;
}
```

**Pola B — objek yang tidak pernah lewat Jackson.** Boleh `final` dan tanpa setter. Dipakai `AppUser` di package `security`.

```java
@Getter
@AllArgsConstructor
public class AppUser implements UserDetails {
    private final String username;
    private final String password;
}
```

```java
// SALAH — record
public record RekeningResponse(String nomor, String bank) {}
```

### 2. Sesederhana mungkin

Buat implementasi paling sederhana yang memenuhi kebutuhan. Jangan menambah lapisan, abstraksi, atau fitur yang tidak diminta.

- Tidak ada interface kalau implementasinya cuma satu dan tidak akan ditest ganda.
- Tidak ada generic, builder, atau factory kalau pemanggilan langsung sudah cukup.
- Tidak ada konfigurasi, flag, atau opsi "buat jaga-jaga".
- Tidak ada error handling spekulatif untuk kondisi yang tidak mungkin terjadi.
- Kalau ragu antara dua rancangan, pilih yang lebih sedikit filenya dan lebih sedikit barisnya.

### 3. Jangan menulis komentar di baris kode

File `.java` tidak boleh berisi komentar — tidak `//`, tidak `/* */`, tidak Javadoc.

- Kode harus jelas lewat penamaan, bukan lewat penjelasan.
- Kalau sebuah baris butuh komentar untuk dimengerti, ganti nama variabel/method atau pecah methodnya.
- Penjelasan konteks ditulis di file markdown, bukan di dalam kode.
- Pengecualian: komentar di `application.properties` dan file `.sql` boleh, itu bukan baris kode Java.

---

## Stack

| Hal | Nilai |
|---|---|
| Java | 21 |
| Framework | Spring Boot 4.1.0 |
| Build | Maven (`./mvnw`) |
| Database | PostgreSQL (schema `masesas`) |
| Cache | Redis |
| Auth | JWT manual via jjwt 0.12.6, STATELESS |
| Boilerplate | Lombok |

## Struktur Package

Root: `com.masesas.exercises.demo1`

```
config/         konfigurasi Spring (security, redis, jdbc, clock)
controller/     REST endpoint aplikasi
dto/            request & response
entity/         JPA entity
model/          model non-JPA
projection/     projection query
repository/     Spring Data + JdbcTemplate + stored procedure
security/       JWT, user details, login attempt
service/        interface service
service/impl/   implementasi service
exception/      exception & handler
```

Letakkan file baru di package yang sudah ada. Jangan bikin package baru kecuali memang tidak ada yang cocok.

## Perintah

```bash
./mvnw clean compile          # kompilasi
./mvnw test                   # semua test
./mvnw test -Dtest=RbacGuestTest  # satu test
./mvnw spring-boot:run        # jalankan aplikasi
```

## Konvensi Kode

- Dependency injection lewat constructor, pakai `@RequiredArgsConstructor` + field `private final`. Jangan `@Autowired` di field.
- Controller tipis: validasi masuk, panggil service, kembalikan `ResponseEntity`. Logika bisnis di service.
- Validasi input pakai `@Valid` + anotasi Bean Validation di DTO.
- Waktu diambil dari bean `Clock` (`ClockConfig`), bukan `Instant.now()` langsung — supaya bisa ditest.
- Query database selalu parameterized. Tidak pernah menyambung string SQL dengan input user.
- Nama file dan class pakai bahasa Inggris untuk istilah teknis, boleh Indonesia untuk istilah domain (`Karyawan`, `Rekening`, `Payroll`).

## Testing

- Setiap perubahan logika wajib disertai test dan `./mvnw test` harus hijau sebelum dianggap selesai.
- Bug fix wajib disertai test yang mereproduksi bug tersebut.
- Test diletakkan mengikuti package kode yang diuji.

## Keamanan

- Jangan menambah kredensial baru ke source code. Semua kredensial dibaca dari `.env` lewat placeholder di `application.properties`.
- `nik` dan `npwp` di `detail_karyawan` terenkripsi di database lewat `security/CryptoConverter`. Jangan melepas `@Convert` tanpa migrasi dekripsi lebih dulu.
- Pesan error yang dikirim ke client tidak boleh membocorkan stack trace atau detail internal.

## Git

Format commit: `<type>: <deskripsi>` — type: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`.

Commit dan push hanya kalau diminta.

## Dokumen Terkait

- [http/README.md](./http/README.md) — matriks akses per peran dan berkas test HTTP
- Materi OWASP Top 10 sudah pindah ke proyek terpisah `top10-owasp`.
