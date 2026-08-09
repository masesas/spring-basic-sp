-- ============================================================================
--  rbac_masesas.sql — tabel dan data awal untuk RBAC (Fase 1)
-- ============================================================================
--
--  Cara pakai:
--
--      HASH="{bcrypt}$(htpasswd -bnBC 12 "" "$DEMO_PASSWORD" | tr -d ':\n')"
--      psql -h <host> -p 5432 -U <user> -d binar_finance \
--           -v pwd_hash="$HASH" -f rbac_masesas.sql
--
--  Hash password TIDAK ditulis di berkas ini. Nilainya dipasok lewat variabel
--  psql :'pwd_hash' saat eksekusi, supaya tidak ada kredensial baru yang ikut
--  ter-commit. Password mentahnya tetap hanya hidup di .env (DEMO_PASSWORD).
--
--  Berkas ini aman dijalankan berulang kali: seluruh DDL memakai IF NOT EXISTS
--  dan seluruh seed memakai ON CONFLICT DO NOTHING.
--
--  Penjelasan rancangannya ada di IMPLEMENTATION-PLAN-RBAC-FASE1.md
-- ============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. Tabel role
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS masesas.role (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nama         varchar(50) NOT NULL,
    created_date timestamp DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_role_nama UNIQUE (nama)
);

-- ---------------------------------------------------------------------------
-- 2. Tabel karyawan_role (satu karyawan boleh punya lebih dari satu peran)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS masesas.karyawan_role (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_karyawan  integer NOT NULL,
    id_role      integer NOT NULL,
    created_date timestamp DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_kr_karyawan FOREIGN KEY (id_karyawan) REFERENCES masesas.karyawan (id) ON DELETE CASCADE,
    CONSTRAINT fk_kr_role     FOREIGN KEY (id_role)     REFERENCES masesas.role (id),
    CONSTRAINT uq_karyawan_role UNIQUE (id_karyawan, id_role)
);

-- ---------------------------------------------------------------------------
-- 3. Tabel customer
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS masesas.customer (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nama         varchar(100) NOT NULL,
    email        varchar(150) NOT NULL,
    password     varchar(100) NOT NULL,
    created_date timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_date timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted_date timestamp,
    CONSTRAINT uq_customer_email UNIQUE (email)
);

-- ---------------------------------------------------------------------------
-- 4. Kolom email dan password pada karyawan
--
--    Keduanya NULLABLE dengan sengaja: Karyawan2Service melakukan
--    INSERT INTO masesas.karyawan (nama, alamat, dob, status, created_date)
--    tanpa mengisi kedua kolom ini. NOT NULL akan mematahkan endpoint itu.
-- ---------------------------------------------------------------------------
ALTER TABLE masesas.karyawan ADD COLUMN IF NOT EXISTS email    varchar(150);
ALTER TABLE masesas.karyawan ADD COLUMN IF NOT EXISTS password varchar(100);

-- ADD CONSTRAINT tidak punya IF NOT EXISTS, jadi keberadaannya diperiksa manual
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_karyawan_email'
          AND conrelid = 'masesas.karyawan'::regclass
    ) THEN
        ALTER TABLE masesas.karyawan ADD CONSTRAINT uq_karyawan_email UNIQUE (email);
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 5. Seed peran
-- ---------------------------------------------------------------------------
INSERT INTO masesas.role (nama)
VALUES ('ADMIN'), ('MANAGER'), ('MARKETING'), ('SALES')
ON CONFLICT (nama) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6. Akun demo karyawan (id 1-6) — password sama untuk semuanya
-- ---------------------------------------------------------------------------
UPDATE masesas.karyawan SET email = 'admin@masesas.test',         password = :'pwd_hash' WHERE id = 1;
UPDATE masesas.karyawan SET email = 'manager@masesas.test',       password = :'pwd_hash' WHERE id = 2;
UPDATE masesas.karyawan SET email = 'marketing@masesas.test',     password = :'pwd_hash' WHERE id = 3;
UPDATE masesas.karyawan SET email = 'sales@masesas.test',         password = :'pwd_hash' WHERE id = 4;
UPDATE masesas.karyawan SET email = 'manager.sales@masesas.test', password = :'pwd_hash' WHERE id = 5;
UPDATE masesas.karyawan SET email = 'tanparole@masesas.test',     password = :'pwd_hash' WHERE id = 6;

-- Sisa karyawan hanya diberi email, password dibiarkan NULL sehingga tidak
-- bisa dipakai login sama sekali.
UPDATE masesas.karyawan
   SET email = 'karyawan' || lpad(id::text, 4, '0') || '@masesas.test'
 WHERE email IS NULL;

-- ---------------------------------------------------------------------------
-- 7. Pemberian peran
--
--    karyawan 5 sengaja diberi dua peran, karyawan 6 sengaja tidak diberi
--    peran sama sekali (login berhasil, akses ditolak 403).
-- ---------------------------------------------------------------------------
INSERT INTO masesas.karyawan_role (id_karyawan, id_role)
SELECT v.id_karyawan, r.id
  FROM (VALUES (1, 'ADMIN'),
               (2, 'MANAGER'),
               (3, 'MARKETING'),
               (4, 'SALES'),
               (5, 'MANAGER'),
               (5, 'SALES')) AS v(id_karyawan, nama)
  JOIN masesas.role r ON r.nama = v.nama
  JOIN masesas.karyawan k ON k.id = v.id_karyawan
ON CONFLICT (id_karyawan, id_role) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 8. Seed customer
-- ---------------------------------------------------------------------------
INSERT INTO masesas.customer (nama, email, password)
VALUES ('Customer Satu', 'customer1@masesas.test', :'pwd_hash'),
       ('Customer Dua',  'customer2@masesas.test', :'pwd_hash')
ON CONFLICT (email) DO NOTHING;

COMMIT;

-- ---------------------------------------------------------------------------
-- Pemeriksaan cepat setelah migrasi dijalankan
-- ---------------------------------------------------------------------------
SELECT 'role' AS tabel, count(*) AS jumlah FROM masesas.role
UNION ALL SELECT 'karyawan_role', count(*) FROM masesas.karyawan_role
UNION ALL SELECT 'customer',      count(*) FROM masesas.customer
UNION ALL SELECT 'karyawan berpassword', count(*) FROM masesas.karyawan WHERE password IS NOT NULL
UNION ALL SELECT 'karyawan beremail',    count(*) FROM masesas.karyawan WHERE email IS NOT NULL;

SELECT k.id, k.email, coalesce(string_agg(r.nama, ', ' ORDER BY r.nama), '(tanpa peran)') AS peran
  FROM masesas.karyawan k
  LEFT JOIN masesas.karyawan_role kr ON kr.id_karyawan = k.id
  LEFT JOIN masesas.role r ON r.id = kr.id_role
 WHERE k.id <= 6
 GROUP BY k.id, k.email
 ORDER BY k.id;
