-- ============================================================================
--  masesas.payroll_karyawan — tambah kolom version (OWASP A08)
--
--  Dipakai JPA @Version untuk optimistic locking. Tanpa kolom ini, dua orang
--  HR yang mengubah slip gaji yang sama secara bersamaan menghasilkan lost
--  update: perubahan yang pertama tertimpa tanpa peringatan apa pun.
--
--  Aman dijalankan berulang kali. Bersifat menambah saja: baris lama mulai
--  dari version 0.
-- ============================================================================

ALTER TABLE masesas.payroll_karyawan
    ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
