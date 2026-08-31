-- ============================================================================
--  permission_customer_masesas.sql — permission baca daftar customer
-- ============================================================================
--
--  Cara pakai:
--
--      psql -h <host> -p 5432 -U <user> -d binar_finance -f permission_customer_masesas.sql
--
--  Jalankan SETELAH permission_masesas.sql — berkas ini menambah satu
--  permission ke katalog yang sudah dibuat di sana, lalu memetakannya ke
--  peran yang sudah ada.
--
--  Tidak ada kredensial yang dibutuhkan maupun ditulis di sini.
--
--  Aman dijalankan berulang kali: seluruh seed memakai ON CONFLICT DO NOTHING.
--
--  CUSTOMER_READ menjaga GET /api/customer. Endpoint itu untuk pegawai, bukan
--  untuk customer: seorang customer tetap hanya bisa membaca dirinya sendiri
--  lewat GET /api/customer/me yang dijaga ROLE_CUSTOMER.
-- ============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. Tambahan katalog permission
-- ---------------------------------------------------------------------------
INSERT INTO masesas.permission (kode, deskripsi)
VALUES ('CUSTOMER_READ', 'Melihat daftar seluruh customer')
ON CONFLICT (kode) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Pemetaan peran ke permission
--
--    SUPERADMIN dan ADMIN memegang seluruh permission, sama seperti di
--    permission_masesas.sql. MANAGER, MARKETING, dan SALES perlu membaca
--    daftar customer untuk memproses pengajuan pinjaman. HR dan KARYAWAN
--    tidak, sejalan dengan pemisahan tugas modul pinjaman.
-- ---------------------------------------------------------------------------
INSERT INTO masesas.role_permission (id_role, id_permission)
SELECT r.id, p.id
  FROM masesas.role r
  CROSS JOIN masesas.permission p
 WHERE r.nama IN ('SUPERADMIN', 'ADMIN')
   AND p.kode = 'CUSTOMER_READ'
ON CONFLICT (id_role, id_permission) DO NOTHING;

INSERT INTO masesas.role_permission (id_role, id_permission)
SELECT r.id, p.id
  FROM masesas.role r
  CROSS JOIN masesas.permission p
 WHERE r.nama IN ('MANAGER', 'MARKETING', 'SALES')
   AND p.kode = 'CUSTOMER_READ'
ON CONFLICT (id_role, id_permission) DO NOTHING;

COMMIT;

-- ---------------------------------------------------------------------------
-- Pemeriksaan setelah migrasi
-- ---------------------------------------------------------------------------
SELECT p.kode, string_agg(r.nama, ', ' ORDER BY r.nama) AS dipegang_peran
  FROM masesas.permission p
  LEFT JOIN masesas.role_permission rp ON rp.id_permission = p.id
  LEFT JOIN masesas.role r ON r.id = rp.id_role
 WHERE p.kode = 'CUSTOMER_READ'
 GROUP BY p.kode;
