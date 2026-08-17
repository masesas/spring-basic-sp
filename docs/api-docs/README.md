# Dokumentasi API

API ini mendokumentasikan dirinya sendiri di `/docs`, memakai
[Scalar](https://scalar.com) sebagai antarmuka dan
[springdoc-openapi](https://springdoc.org) sebagai pembangkit spesifikasi.
Tidak ada Swagger UI di proyek ini.

## Alamat

| Path | Isi |
|---|---|
| `/docs` | halaman dokumentasi interaktif |
| `/docs/openapi` | spesifikasi OpenAPI 3.1 dalam JSON |
| `/docs/openapi.yaml` | spesifikasi yang sama dalam YAML |
| `/docs/scalar.js` | bundle antarmuka, disajikan dari jar, bukan dari CDN |

Terbuka tanpa token, aktif di semua profile. Untuk mematikannya di satu
lingkungan, cukup `scalar.enabled=false` di `application-<profile>.properties` —
tanpa mengubah kode.

## Login di halaman dokumentasi

Token tidak perlu ditempel manual.

1. Klik **Authorize**.
2. Pilih skema: `karyawanAuth` untuk akun karyawan, `customerAuth` untuk customer.
3. Isi username dan password, lalu submit.

Scalar memanggil endpoint token, membaca `access_token` dari balasannya, dan
memasang `Authorization: Bearer ...` ke setiap permintaan **Try it** berikutnya.
Karena `scalar.persist-auth=true`, token itu bertahan saat halaman dimuat ulang.

Yang terjadi di baliknya adalah OAuth2 password flow biasa:

```
POST /api/auth/karyawan/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&username=hr@masesas.test&password=...
```

```json
{ "access_token": "eyJhbGciOiJIUzI1NiJ9...", "token_type": "Bearer", "expires_in": 900 }
```

Endpoint token memakai ulang pemeriksaan kredensial dan penguncian akun yang
sama persis dengan `/api/auth/karyawan/login`. Yang berbeda hanya content-type
dan bentuk balasannya — bentuk baku OAuth2 dipilih supaya Scalar bisa membaca
tokennya tanpa konfigurasi tambahan.

Scalar tidak punya pre/post-request script seperti Postman. Password flow ini
satu-satunya jalur otomatis yang tersedia, dan itulah sebabnya endpoint token
menerima form-urlencoded, bukan JSON seperti endpoint login lainnya.

## Dari mana isi dokumentasinya

Tidak ada berkas spesifikasi yang ditulis tangan. Seluruh isi `/docs/openapi`
dibangkitkan saat aplikasi berjalan dari:

| Sumber | Menghasilkan |
|---|---|
| `config/OpenApiConfig` | judul, deskripsi, dan dua security scheme |
| `@Tag` di controller | pengelompokan endpoint |
| `@Operation`, `@ApiResponse`, `@Parameter` di method | penjelasan tiap endpoint |
| `@SecurityRequirement` | endpoint mana butuh skema yang mana |
| `@Schema` di DTO dan model | penjelasan tiap field beserta contohnya |
| Bean Validation (`@NotBlank`, `@Size`, `@Pattern`) | penanda wajib dan batasan nilai |
| `@ApiResponse` di `GlobalExceptionHandler` | kontrak error 400/401/403/404/409/422/500 |

Baris terakhir itu yang membuat dokumentasi ini tidak berulang: kontrak error
ditulis sekali di tempat error itu dibuat, lalu springdoc melekatkannya ke
seluruh operation. Menambah `@ExceptionHandler` baru otomatis menambah kode
error di semua endpoint — tidak ada daftar terpisah yang bisa ketinggalan.

## Menjaganya tetap benar

`config/DocsAccessTest` menjaga hal-hal yang mudah rusak diam-diam:

- `/docs`, `/docs/openapi`, dan `/docs/scalar.js` terbuka tanpa token
- CSP di `/docs` cukup longgar untuk menjalankan Scalar
- CSP di `/api/**` **tidak** ikut longgar
- kedua security scheme menunjuk ke endpoint token yang benar
- setiap operation punya `summary` — endpoint baru tanpa `@Operation` membuat
  test gagal, bukan lolos diam-diam
- kontrak error dan skema `ApiError` melekat ke operation

## Batasan yang diketahui

- Halaman `/docs` butuh `script-src 'unsafe-inline'` karena template Scalar
  memanggil `Scalar.createApiReference()` lewat script inline. Kelonggaran ini
  hanya berlaku di `/docs`; `/api/**` tetap `default-src 'none'`.
- Skema `Pageable`, `PageMetadata`, dan `PagedModel*` dibangkitkan springdoc
  sendiri sehingga fieldnya tidak berdeskripsi. Bentuknya sudah diverifikasi
  cocok dengan JSON yang benar-benar dikirim.
- Dokumentasi terbuka tanpa login di semua lingkungan, termasuk produksi. Ini
  keputusan yang diambil sadar; cara membatasinya ada di bagian Alamat.

## Rujukan

- [impl-plan-scalar.md](./impl-plan-scalar.md) — rencana, keputusan, dan hasil verifikasinya
- [../../http/README.md](../../http/README.md) — matriks akses per peran
