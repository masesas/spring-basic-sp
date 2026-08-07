-- ============================================================================
--  seeder.sql — data contoh untuk latihan (1000 baris per tabel)
-- ============================================================================
--
--  Cara pakai:
--      psql -h 129.226.195.9 -p 5432 -U binar_admin -d binar_finance -f seeder.sql
--
--  Catatan penting:
--    * Kolom "id" di schema masesas TIDAK punya sequence/identity, jadi setiap
--      id di sini diisi eksplisit 1..1000. Relasi antar tabel memakai id yang
--      sama (karyawan 7 -> detail_karyawan 7 -> rekening 7 -> dst) supaya
--      gampang ditelusuri saat latihan.
--    * Semua statement memakai ON CONFLICT DO NOTHING, jadi file ini aman
--      dijalankan berulang kali tanpa menghasilkan duplikat atau error.
--    * Urutan insert mengikuti arah foreign key:
--      detail_karyawan -> karyawan -> training -> rekening -> karyawan_training
--    * deleted_date dibiarkan NULL (semua baris aktif). Lihat bagian OPSIONAL
--      di bawah kalau ingin sebagian baris berstatus terhapus (soft delete).
-- ============================================================================

set search_path = masesas;


BEGIN;

-- ---------------------------------------------------------------------------
-- 1. detail_karyawan  (nik dan npwp wajib unik)
-- ---------------------------------------------------------------------------
INSERT INTO masesas.detail_karyawan (id, nik, npwp, created_date, updated_date, deleted_date)
SELECT i,
       '3273' || lpad(i::text, 12, '0'),
       '09' || lpad(i::text, 13, '0'),
       timestamp '2024-01-01 08:00:00' + make_interval(mins => i),
       timestamp '2024-01-01 08:00:00' + make_interval(mins => i),
       NULL
FROM generate_series(1, 1000) AS i
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. karyawan  (kolom detail_karyawan unik -> hubungan satu-ke-satu)
-- ---------------------------------------------------------------------------
INSERT INTO masesas.karyawan (id, nama, alamat, dob, status, detail_karyawan,
                             created_date, updated_date, deleted_date)
SELECT i,
       'Karyawan ' || lpad(i::text, 4, '0'),
       'Jl. Contoh No. ' || i || ', ' ||
           (ARRAY['Jakarta', 'Bandung', 'Surabaya', 'Medan', 'Makassar'])[1 + (i % 5)],
       date '1970-01-01' + (i * 11),
       CASE WHEN i % 10 = 0 THEN 'NONAKTIF' ELSE 'AKTIF' END,
       i,
       timestamp '2024-01-01 08:00:00' + make_interval(mins => i),
       timestamp '2024-01-01 08:00:00' + make_interval(mins => i),
       NULL
FROM generate_series(1, 1000) AS i
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3. training
-- ---------------------------------------------------------------------------
INSERT INTO masesas.training (id, tema, pengajar, created_date, updated_date, deleted_date)
SELECT i,
       (ARRAY['Java Dasar', 'Spring Boot', 'Basis Data', 'REST API',
              'Redis dan Caching', 'Pengujian Unit', 'Git dan Kolaborasi',
              'Keamanan Aplikasi'])[1 + (i % 8)] || ' — Batch ' || lpad(i::text, 4, '0'),
       'Pengajar ' || lpad(i::text, 4, '0'),
       timestamp '2024-01-01 08:00:00' + make_interval(mins => i),
       timestamp '2024-01-01 08:00:00' + make_interval(mins => i),
       NULL
FROM generate_series(1, 1000) AS i
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. rekening  (satu rekening per karyawan)
-- ---------------------------------------------------------------------------
INSERT INTO masesas.rekening (id, id_karyawan, jenis, nama, rekening,
                             created_date, updated_date, deleted_date)
SELECT i,
       i,
       (ARRAY['BCA', 'BNI', 'BRI', 'MANDIRI'])[1 + (i % 4)],
       'Karyawan ' || lpad(i::text, 4, '0'),
       lpad(i::text, 10, '0'),
       timestamp '2024-01-01 08:00:00' + make_interval(mins => i),
       timestamp '2024-01-01 08:00:00' + make_interval(mins => i),
       NULL
FROM generate_series(1, 1000) AS i
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 5. karyawan_training  (karyawan ke-i mengikuti training ke-i)
-- ---------------------------------------------------------------------------
INSERT INTO masesas.karyawan_training (id, id_karyawan, id_training, tanggal,
                                      created_date, updated_date, deleted_date)
SELECT i,
       i,
       i,
       date '2024-01-01' + (i % 365),
       timestamp '2024-01-01 08:00:00' + make_interval(mins => i),
       timestamp '2024-01-01 08:00:00' + make_interval(mins => i),
       NULL
FROM generate_series(1, 1000) AS i
ON CONFLICT DO NOTHING;

COMMIT;

-- ---------------------------------------------------------------------------
-- Pemeriksaan cepat setelah seeder dijalankan
-- ---------------------------------------------------------------------------
SELECT 'detail_karyawan'   AS tabel, count(*) AS jumlah FROM masesas.detail_karyawan
UNION ALL SELECT 'karyawan',          count(*) FROM masesas.karyawan
UNION ALL SELECT 'training',          count(*) FROM masesas.training
UNION ALL SELECT 'rekening',          count(*) FROM masesas.rekening
UNION ALL SELECT 'karyawan_training', count(*) FROM masesas.karyawan_training;


-- ============================================================================
--  OPSIONAL — jalankan manual kalau dibutuhkan
-- ============================================================================

-- (a) Menandai sebagian baris sebagai terhapus, untuk melatih filter soft delete.
--     Setelah ini, endpoint GET hanya akan menampilkan 980 dari 1000 karyawan.
--
-- UPDATE masesas.karyawan
--    SET deleted_date = timestamp '2024-06-01 00:00:00'
--  WHERE id % 50 = 0;

-- (b) Mengosongkan kembali seluruh data contoh.
--     Urutannya dibalik dari urutan insert supaya tidak melanggar foreign key.
--
-- BEGIN;
-- DELETE FROM masesas.karyawan_training;
-- DELETE FROM masesas.rekening;
-- DELETE FROM masesas.training;
-- DELETE FROM masesas.karyawan;
-- DELETE FROM masesas.detail_karyawan;
-- COMMIT;

-- (c) Membuat kolom id bisa mengisi dirinya sendiri.
--     Saat ini kolom id di schema masesas tidak punya sequence sama sekali,
--     sehingga INSERT dari aplikasi gagal dengan pesan:
--         null value in column "id" ... violates not-null constraint
--     Blok di bawah memasang identity dan memulainya dari 1001 supaya tidak
--     bentrok dengan data seeder. Jalankan kalau ingin endpoint POST berfungsi.
--
-- BEGIN;
-- ALTER TABLE masesas.detail_karyawan   ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (START WITH 1001);
-- ALTER TABLE masesas.karyawan          ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (START WITH 1001);
-- ALTER TABLE masesas.training          ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (START WITH 1001);
-- ALTER TABLE masesas.rekening          ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (START WITH 1001);
-- ALTER TABLE masesas.karyawan_training ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (START WITH 1001);
-- COMMIT;
