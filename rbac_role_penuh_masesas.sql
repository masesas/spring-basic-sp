-- ============================================================================
--  rbac_role_penuh_masesas.sql — RBAC Fase 3: seluruh peran bersumber dari database
-- ============================================================================
--
--  Cara pakai:
--
--      psql -h <host> -p 5432 -U <user> -d binar_finance -f rbac_role_penuh_masesas.sql
--
--  Tidak ada variabel yang perlu dipasok. Berbeda dari rbac_masesas.sql (Fase 1)
--  yang menuntut -v pwd_hash, berkas ini menyalin hash password dari akun demo
--  yang sudah ada (karyawan id 1). Akun baru karena itu otomatis memakai
--  DEMO_PASSWORD yang sama tanpa hash apa pun menyentuh berkas ini, shell
--  history, maupun daftar proses.
--
--  Catatan: seluruh berkas berjalan dalam satu transaksi. Bila ada satu
--  pernyataan gagal, TIDAK ADA perubahan yang tersimpan — bukan sebagian.
--
--  Berkas ini menggantikan tiga akun demo yang sebelumnya di-hardcode di
--  AppUserDetailsService (admin, hr, karyawan) dengan akun database sungguhan.
--  Jalankan SEBELUM ./mvnw test — test OWASP sekarang login lewat akun ini.
--
--  Aman dijalankan berulang kali: seluruh INSERT memakai ON CONFLICT DO NOTHING
--  dan seluruh UPDATE menulis nilai yang sama bila diulang.
--
--  Penjelasan rancangannya ada di IMPLEMENTATION-PLAN-RBAC-FASE3.md
-- ============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. Dua peran yang selama ini hanya hidup di kode
--
--    HR dirujuk 4 tempat @PreAuthorize dan KARYAWAN 1 tempat, tetapi tidak
--    pernah ada di tabel role. Akibatnya hasRole('HR') tidak pernah cocok
--    dengan satu pun pengguna database.
-- ---------------------------------------------------------------------------
INSERT INTO masesas.role (nama)
VALUES ('HR'), ('KARYAWAN')
ON CONFLICT (nama) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Akun pengganti demo user in-memory
--
--    Karyawan 6 (tanparole@masesas.test) sengaja TIDAK disentuh: RbacKaryawanTest
--    bergantung pada adanya karyawan yang login berhasil tetapi tanpa peran
--    sama sekali.
-- ---------------------------------------------------------------------------
--    Hash disalin dari karyawan id 1 supaya password kedua akun ini sama dengan
--    akun demo lainnya (DEMO_PASSWORD). Bila id 1 belum punya password, berarti
--    rbac_masesas.sql (Fase 1) belum dijalankan — migrasi ini berhenti dengan
--    pesan yang jelas, bukan diam-diam membuat akun tanpa password.
DO $$
DECLARE
    hash_demo text;
BEGIN
    SELECT password INTO hash_demo FROM masesas.karyawan WHERE id = 1;

    IF hash_demo IS NULL THEN
        RAISE EXCEPTION 'karyawan id 1 belum punya password — jalankan rbac_masesas.sql lebih dulu';
    END IF;

    UPDATE masesas.karyawan SET email = 'hr@masesas.test',       password = hash_demo WHERE id = 7;
    UPDATE masesas.karyawan SET email = 'karyawan@masesas.test', password = hash_demo WHERE id = 8;
END
$$;

-- ---------------------------------------------------------------------------
-- 3. Pemberian peran
-- ---------------------------------------------------------------------------
INSERT INTO masesas.karyawan_role (id_karyawan, id_role)
SELECT v.id_karyawan, r.id
  FROM (VALUES (7, 'HR'),
               (8, 'KARYAWAN')) AS v(id_karyawan, nama)
  JOIN masesas.role r     ON r.nama = v.nama
  JOIN masesas.karyawan k ON k.id = v.id_karyawan
ON CONFLICT (id_karyawan, id_role) DO NOTHING;

COMMIT;

-- ---------------------------------------------------------------------------
-- Pemeriksaan setelah migrasi — jalankan sebelum ./mvnw test
--
-- Yang harus terlihat:
--   * enam peran: ADMIN, GUEST tidak ada (lihat catatan di bawah), HR, KARYAWAN,
--     MANAGER, MARKETING, SALES
--   * hr@masesas.test        -> HR
--   * karyawan@masesas.test  -> KARYAWAN
--   * tanparole@masesas.test -> (tanpa peran)
--
-- GUEST sengaja TIDAK ada di tabel ini. Guest adalah pengunjung anonim tanpa
-- akun, jadi tidak ada baris karyawan yang bisa ditugasi peran itu. Peran
-- tersebut diberikan Spring Security lewat AnonymousConfigurer di SecurityConfig.
-- ---------------------------------------------------------------------------
SELECT nama FROM masesas.role ORDER BY nama;

SELECT k.id,
       k.email,
       coalesce(string_agg(r.nama, ', ' ORDER BY r.nama), '(tanpa peran)') AS peran,
       (k.password IS NOT NULL) AS bisa_login
  FROM masesas.karyawan k
  LEFT JOIN masesas.karyawan_role kr ON kr.id_karyawan = k.id
  LEFT JOIN masesas.role r ON r.id = kr.id_role
 WHERE k.id <= 8
 GROUP BY k.id, k.email, k.password
 ORDER BY k.id;
