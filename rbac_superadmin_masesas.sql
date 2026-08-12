-- ============================================================================
--  rbac_superadmin_masesas.sql — peran SUPERADMIN
-- ============================================================================
--
--  Cara pakai:
--
--      psql -h <host> -p 5432 -U <user> -d binar_finance -f rbac_superadmin_masesas.sql
--
--  Jalankan SETELAH rbac_masesas.sql dan rbac_role_penuh_masesas.sql.
--
--  Tidak ada variabel yang perlu dipasok. Seperti rbac_role_penuh_masesas.sql,
--  berkas ini menyalin hash password dari karyawan id 1, jadi akun baru memakai
--  DEMO_PASSWORD yang sama tanpa hash apa pun menyentuh berkas ini.
--
--  SUPERADMIN tidak muncul di satu pun @PreAuthorize. Haknya datang dari
--  RoleHierarchy di SecurityConfig: ROLE_SUPERADMIN mengimplikasikan seluruh
--  peran lain yang ada di tabel ini ditambah CUSTOMER. Konsekuensinya, peran
--  yang ditambahkan ke tabel role SETELAH aplikasi jalan baru ikut terimplikasi
--  setelah aplikasi di-restart — hierarki dibangun sekali saat startup.
--
--  Aman dijalankan berulang kali: INSERT memakai ON CONFLICT DO NOTHING dan
--  UPDATE menulis nilai yang sama bila diulang.
-- ============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. Peran
-- ---------------------------------------------------------------------------
INSERT INTO masesas.role (nama)
VALUES ('SUPERADMIN')
ON CONFLICT (nama) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Akun superadmin@masesas.test
--
--    Karyawan id 9 dipakai ulang, mengikuti pola id 7 (HR) dan id 8 (KARYAWAN)
--    di rbac_role_penuh_masesas.sql. Baris itu sebelumnya tidak punya password
--    dan tidak punya peran, jadi tidak ada test yang bergantung padanya.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    hash_demo text;
BEGIN
    SELECT password INTO hash_demo FROM masesas.karyawan WHERE id = 1;

    IF hash_demo IS NULL THEN
        RAISE EXCEPTION 'karyawan id 1 belum punya password — jalankan rbac_masesas.sql lebih dulu';
    END IF;

    UPDATE masesas.karyawan SET email = 'superadmin@masesas.test', password = hash_demo WHERE id = 9;
END
$$;

-- ---------------------------------------------------------------------------
-- 3. Pemberian peran
-- ---------------------------------------------------------------------------
INSERT INTO masesas.karyawan_role (id_karyawan, id_role)
SELECT k.id, r.id
  FROM masesas.karyawan k
  JOIN masesas.role r ON r.nama = 'SUPERADMIN'
 WHERE k.id = 9
ON CONFLICT (id_karyawan, id_role) DO NOTHING;

COMMIT;

-- ---------------------------------------------------------------------------
-- Pemeriksaan setelah migrasi
--
-- Yang harus terlihat:
--   * tujuh peran: ADMIN, HR, KARYAWAN, MANAGER, MARKETING, SALES, SUPERADMIN
--   * superadmin@masesas.test -> SUPERADMIN, bisa_login = t
-- ---------------------------------------------------------------------------
SELECT nama FROM masesas.role ORDER BY nama;

SELECT k.id,
       k.email,
       coalesce(string_agg(r.nama, ', ' ORDER BY r.nama), '(tanpa peran)') AS peran,
       (k.password IS NOT NULL) AS bisa_login
  FROM masesas.karyawan k
  LEFT JOIN masesas.karyawan_role kr ON kr.id_karyawan = k.id
  LEFT JOIN masesas.role r ON r.id = kr.id_role
 WHERE k.id = 9
 GROUP BY k.id, k.email, k.password;
