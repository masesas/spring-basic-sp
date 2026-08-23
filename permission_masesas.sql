-- ============================================================================
--  permission_masesas.sql — permission berbutir halus di atas RBAC
-- ============================================================================
--
--  Cara pakai:
--
--      psql -h <host> -p 5432 -U <user> -d binar_finance -f permission_masesas.sql
--
--  Jalankan SETELAH rbac_masesas.sql, rbac_role_penuh_masesas.sql, dan
--  rbac_superadmin_masesas.sql — berkas ini memetakan permission ke tujuh peran
--  yang dibuat ketiga berkas itu.
--
--  Tidak ada kredensial yang dibutuhkan maupun ditulis di sini.
--
--  Aman dijalankan berulang kali: seluruh DDL memakai IF NOT EXISTS dan seluruh
--  seed memakai ON CONFLICT DO NOTHING.
--
--  Catatan penting soal SUPERADMIN: RoleHierarchy di SecurityConfig hanya
--  mengimplikasikan authority berawalan ROLE_. Permission TIDAK berawalan ROLE_,
--  jadi SUPERADMIN tidak otomatis mewarisinya — permission-nya diberikan
--  eksplisit di bagian 4.
-- ============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. Tabel permission
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS masesas.permission (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kode         varchar(60) NOT NULL,
    deskripsi    varchar(150),
    created_date timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_date timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted_date timestamp,
    CONSTRAINT uq_permission_kode UNIQUE (kode)
);

-- ---------------------------------------------------------------------------
-- 2. Tabel role_permission
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS masesas.role_permission (
    id            integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_role       integer NOT NULL,
    id_permission integer NOT NULL,
    created_date  timestamp DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rp_role       FOREIGN KEY (id_role)       REFERENCES masesas.role (id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (id_permission) REFERENCES masesas.permission (id) ON DELETE CASCADE,
    CONSTRAINT uq_role_permission UNIQUE (id_role, id_permission)
);

CREATE INDEX IF NOT EXISTS idx_role_permission_role ON masesas.role_permission (id_role);

-- ---------------------------------------------------------------------------
-- 3. Katalog permission
--
--    Pola penamaan: <RESOURCE>_<AKSI>. READ untuk seluruh GET, WRITE untuk
--    POST/PUT/DELETE. Transisi status loan_application punya permission
--    sendiri-sendiri supaya "siapa boleh approve" bisa diubah tanpa deploy.
-- ---------------------------------------------------------------------------
INSERT INTO masesas.permission (kode, deskripsi)
VALUES ('BRANCH_READ',              'Melihat data cabang'),
       ('BRANCH_WRITE',             'Menambah, mengubah, dan menghapus cabang'),
       ('PERMISSION_READ',          'Melihat katalog permission'),
       ('PERMISSION_WRITE',         'Menambah, mengubah, dan menghapus permission'),
       ('ROLE_PERMISSION_READ',     'Melihat pemetaan peran ke permission'),
       ('ROLE_PERMISSION_WRITE',    'Memberi dan mencabut permission dari peran'),
       ('LOAN_PRODUCT_READ',        'Melihat produk pinjaman'),
       ('LOAN_PRODUCT_WRITE',       'Menambah, mengubah, dan menghapus produk pinjaman'),
       ('LOAN_DOCUMENT_TYPE_READ',  'Melihat jenis dokumen pinjaman'),
       ('LOAN_DOCUMENT_TYPE_WRITE', 'Menambah, mengubah, dan menghapus jenis dokumen pinjaman'),
       ('LOAN_PLAFOND_READ',        'Melihat plafond kredit customer'),
       ('LOAN_PLAFOND_WRITE',       'Menetapkan dan mengubah plafond kredit customer'),
       ('LOAN_APPLICATION_READ',    'Melihat seluruh pengajuan pinjaman'),
       ('LOAN_APPLICATION_APPROVE', 'Menyetujui pengajuan pinjaman berstatus SUBMITTED'),
       ('LOAN_APPLICATION_REJECT',  'Menolak pengajuan pinjaman berstatus SUBMITTED'),
       ('LOAN_APPLICATION_DISBURSE','Mencairkan pengajuan pinjaman berstatus APPROVED'),
       ('LOAN_PAYMENT_READ',        'Melihat pembayaran angsuran'),
       ('LOAN_PAYMENT_WRITE',       'Mencatat dan menghapus pembayaran angsuran')
ON CONFLICT (kode) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. Pemetaan peran ke permission
--
--    SUPERADMIN dan ADMIN memegang seluruh permission. Sisanya ditulis
--    eksplisit supaya pemisahan tugasnya terbaca: MANAGER boleh menyetujui
--    dan menolak tetapi tidak boleh mencairkan; SALES dan MARKETING hanya
--    membaca; HR dan KARYAWAN tidak menyentuh modul pinjaman sama sekali.
-- ---------------------------------------------------------------------------
INSERT INTO masesas.role_permission (id_role, id_permission)
SELECT r.id, p.id
  FROM masesas.role r
  CROSS JOIN masesas.permission p
 WHERE r.nama IN ('SUPERADMIN', 'ADMIN')
ON CONFLICT (id_role, id_permission) DO NOTHING;

INSERT INTO masesas.role_permission (id_role, id_permission)
SELECT r.id, p.id
  FROM (VALUES
        ('MANAGER',   'BRANCH_READ'),
        ('MANAGER',   'LOAN_PRODUCT_READ'),
        ('MANAGER',   'LOAN_DOCUMENT_TYPE_READ'),
        ('MANAGER',   'LOAN_PLAFOND_READ'),
        ('MANAGER',   'LOAN_APPLICATION_READ'),
        ('MANAGER',   'LOAN_APPLICATION_APPROVE'),
        ('MANAGER',   'LOAN_APPLICATION_REJECT'),
        ('MANAGER',   'LOAN_PAYMENT_READ'),
        ('MARKETING', 'BRANCH_READ'),
        ('MARKETING', 'LOAN_PRODUCT_READ'),
        ('MARKETING', 'LOAN_DOCUMENT_TYPE_READ'),
        ('MARKETING', 'LOAN_APPLICATION_READ'),
        ('SALES',     'BRANCH_READ'),
        ('SALES',     'LOAN_PRODUCT_READ'),
        ('SALES',     'LOAN_DOCUMENT_TYPE_READ'),
        ('SALES',     'LOAN_PLAFOND_READ'),
        ('SALES',     'LOAN_APPLICATION_READ'),
        ('SALES',     'LOAN_PAYMENT_READ'),
        ('SALES',     'LOAN_PAYMENT_WRITE')
       ) AS v(nama_role, kode_permission)
  JOIN masesas.role r       ON r.nama = v.nama_role
  JOIN masesas.permission p ON p.kode = v.kode_permission
ON CONFLICT (id_role, id_permission) DO NOTHING;

COMMIT;

-- ---------------------------------------------------------------------------
-- Pemeriksaan setelah migrasi
-- ---------------------------------------------------------------------------
SELECT r.nama AS peran, count(rp.id) AS jumlah_permission
  FROM masesas.role r
  LEFT JOIN masesas.role_permission rp ON rp.id_role = r.id
 GROUP BY r.nama
 ORDER BY r.nama;

SELECT p.kode, string_agg(r.nama, ', ' ORDER BY r.nama) AS dipegang_peran
  FROM masesas.permission p
  LEFT JOIN masesas.role_permission rp ON rp.id_permission = p.id
  LEFT JOIN masesas.role r ON r.id = rp.id_role
 WHERE p.kode LIKE 'LOAN_APPLICATION%'
 GROUP BY p.kode
 ORDER BY p.kode;
