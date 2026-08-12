-- ============================================================================
--  masesas.karyawan — tambah kolom avatar
--
--  Kolom menyimpan lokasi relatif berkas gambar terhadap app.image.base-dir,
--  contoh: "karyawan/9f3c1e2a-....png". Isi berkasnya sendiri tidak masuk
--  database — hanya penunjuknya, supaya baris karyawan tetap ringan.
--
--  Aman dijalankan berulang kali.
-- ============================================================================

ALTER TABLE masesas.karyawan
    ADD COLUMN IF NOT EXISTS avatar varchar(255);
