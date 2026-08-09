# CLAUDE.md

Acuan standar dan aturan kerja untuk proyek `demo1`. Berlaku untuk semua kode baru di repositori ini.

---

## Aturan Wajib

Tiga aturan berikut tidak boleh dilanggar.

### 1. Jangan pernah membuat `record` baru

Semua tipe baru (DTO, model, value object) ditulis sebagai **class biasa**, bukan `record`.

- `record` yang **sudah ada** dibiarkan apa adanya — jangan dikonversi, jangan disentuh kecuali memang bagian dari perubahan yang diminta.
- Konsekuensinya: pemanggil `record` lama tetap memakai accessor gaya record (`request.username()`), sedangkan class baru memakai getter biasa. Ini normal, jangan diseragamkan.

```java
// SALAH — record baru
public record RekeningResponse(String nomor, String bank) {}

// BENAR — class + Lombok
@Getter
@AllArgsConstructor
public class RekeningResponse {
    private final String nomor;
    private final String bank;
}
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
- Penjelasan konteks ditulis di file markdown (`TOP10-OWASP.md`, `IMPLEMENTATION-PLAN.md`), bukan di dalam kode.
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
owasp/vuln/     versi RENTAN (dipagari @Profile("owasp-demo"))
owasp/safe/     versi AMAN
```

Letakkan file baru di package yang sudah ada. Jangan bikin package baru kecuali memang tidak ada yang cocok.

## Perintah

```bash
./mvnw clean compile          # kompilasi
./mvnw test                   # semua test
./mvnw test -Dtest=A07AuthTest   # satu test
./mvnw spring-boot:run        # jalankan aplikasi

# menyalakan endpoint rentan — HANYA di komputer sendiri
./mvnw spring-boot:run -Dspring-boot.run.profiles=owasp-demo
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
- Test OWASP menguji dua sisi: eksploit **berhasil** di endpoint `vuln`, dan **gagal** di endpoint `safe`.
- Test diletakkan mengikuti package kode yang diuji.

## Keamanan

- Package `owasp/vuln` berisi kerentanan sungguhan. Selalu pagari dengan `@Profile("owasp-demo")`. Jangan pernah deploy dengan profil ini menyala.
- Jangan menambah kredensial baru ke source code. Kredensial yang saat ini masih ada di `application.properties` akan dipindahkan ke `.env` pada tahap A05.
- Pesan error yang dikirim ke client tidak boleh membocorkan stack trace atau detail internal.

## Git

Format commit: `<type>: <deskripsi>` — type: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`.

Commit dan push hanya kalau diminta.

## Dokumen Terkait

- [TOP10-OWASP.md](./TOP10-OWASP.md) — penjelasan tiap kategori OWASP dan peta lokasi implementasinya
- [IMPLEMENTATION-PLAN.md](./IMPLEMENTATION-PLAN.md) — rencana pengerjaan bertahap
