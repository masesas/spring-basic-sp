-- ============================================================================
--  masesas.detail_karyawan — perlebar kolom nik/npwp (OWASP A02)
--
--  NIK dan NPWP akan disimpan terenkripsi AES-GCM lalu di-Base64. Nilai 16
--  karakter berubah jadi sekitar 70 karakter setelah ditambah IV, tag
--  autentikasi, dan penanda versi. varchar(20) tidak akan muat.
--
--  Aman dijalankan berulang kali. Memperlebar kolom tidak pernah menolak data
--  yang sudah ada, jadi baris lama tetap utuh dan masih terbaca sebagai teks
--  biasa sampai dienkripsi oleh runner migrasi.
-- ============================================================================

ALTER TABLE masesas.detail_karyawan
    ALTER COLUMN nik TYPE varchar(255);

ALTER TABLE masesas.detail_karyawan
    ALTER COLUMN npwp TYPE varchar(255);
