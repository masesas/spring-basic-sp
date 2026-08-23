-- ============================================================================
--  loan_masesas.sql — master dan transaksi modul pinjaman
-- ============================================================================
--
--  Cara pakai:
--
--      psql -h <host> -p 5432 -U <user> -d binar_finance -f loan_masesas.sql
--
--  Jalankan SETELAH permission_masesas.sql. Tidak ada kredensial yang
--  dibutuhkan maupun ditulis di sini.
--
--  Aman dijalankan berulang kali: seluruh DDL memakai IF NOT EXISTS dan seluruh
--  seed memakai ON CONFLICT DO NOTHING.
--
--  Alur status loan_application:
--
--      DRAFT --submit--> SUBMITTED --approve--> APPROVED --disburse--> DISBURSED
--                            |
--                         reject--> REJECTED
--
--      DRAFT dan SUBMITTED juga bisa dibatalkan customer menjadi CANCELLED.
--      Transisi ditegakkan di entity LoanApplication, bukan di database.
-- ============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. Master: cabang
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS masesas.branch (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kode         varchar(20) NOT NULL,
    nama         varchar(100) NOT NULL,
    alamat       text,
    created_date timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_date timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted_date timestamp,
    CONSTRAINT uq_branch_kode UNIQUE (kode)
);

-- ---------------------------------------------------------------------------
-- 2. Master: produk pinjaman
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS masesas.loan_product (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kode         varchar(20) NOT NULL,
    nama         varchar(100) NOT NULL,
    bunga_persen numeric(5,2) NOT NULL,
    tenor_min    integer NOT NULL,
    tenor_max    integer NOT NULL,
    plafond_min  numeric(18,2) NOT NULL,
    plafond_max  numeric(18,2) NOT NULL,
    aktif        boolean NOT NULL DEFAULT true,
    created_date timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_date timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted_date timestamp,
    CONSTRAINT uq_loan_product_kode UNIQUE (kode),
    CONSTRAINT ck_loan_product_bunga   CHECK (bunga_persen >= 0),
    CONSTRAINT ck_loan_product_tenor   CHECK (tenor_min > 0 AND tenor_max >= tenor_min),
    CONSTRAINT ck_loan_product_plafond CHECK (plafond_min >= 0 AND plafond_max >= plafond_min)
);

-- ---------------------------------------------------------------------------
-- 3. Master: jenis dokumen pinjaman
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS masesas.loan_document_type (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kode         varchar(30) NOT NULL,
    nama         varchar(100) NOT NULL,
    wajib        boolean NOT NULL DEFAULT false,
    created_date timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_date timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted_date timestamp,
    CONSTRAINT uq_loan_document_type_kode UNIQUE (kode)
);

-- ---------------------------------------------------------------------------
-- 4. Transaksi: plafond kredit per customer
--
--    Satu baris per customer. Sisa plafond tidak disimpan sebagai kolom karena
--    ia selalu bisa dihitung: plafond_total - plafond_terpakai. Menyimpannya
--    berarti tiga angka yang harus dijaga tetap konsisten, bukan dua.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS masesas.loan_plafond (
    id               integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_customer      integer NOT NULL,
    plafond_total    numeric(18,2) NOT NULL DEFAULT 0,
    plafond_terpakai numeric(18,2) NOT NULL DEFAULT 0,
    version          bigint NOT NULL DEFAULT 0,
    created_date     timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_date     timestamp DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_plafond_customer FOREIGN KEY (id_customer) REFERENCES masesas.customer (id) ON DELETE CASCADE,
    CONSTRAINT uq_plafond_customer UNIQUE (id_customer),
    CONSTRAINT ck_plafond_nilai CHECK (
        plafond_total >= 0
        AND plafond_terpakai >= 0
        AND plafond_terpakai <= plafond_total)
);

-- ---------------------------------------------------------------------------
-- 5. Transaksi: pengajuan pinjaman
--
--    Pengajuan dibuat customer untuk dirinya sendiri; id_customer diambil dari
--    token, tidak pernah dari badan permintaan.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS masesas.loan_application (
    id               integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_customer      integer NOT NULL,
    id_loan_product  integer NOT NULL,
    id_branch        integer,
    jumlah_pengajuan numeric(18,2) NOT NULL,
    tenor_bulan      integer NOT NULL,
    status           varchar(20) NOT NULL DEFAULT 'DRAFT',
    catatan          varchar(255),
    version          bigint NOT NULL DEFAULT 0,
    created_date     timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_date     timestamp DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_application_customer FOREIGN KEY (id_customer)     REFERENCES masesas.customer (id),
    CONSTRAINT fk_application_product  FOREIGN KEY (id_loan_product) REFERENCES masesas.loan_product (id),
    CONSTRAINT fk_application_branch   FOREIGN KEY (id_branch)       REFERENCES masesas.branch (id),
    CONSTRAINT ck_application_jumlah   CHECK (jumlah_pengajuan > 0),
    CONSTRAINT ck_application_tenor    CHECK (tenor_bulan > 0),
    CONSTRAINT ck_application_status   CHECK (status IN
        ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'DISBURSED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_application_customer ON masesas.loan_application (id_customer);
CREATE INDEX IF NOT EXISTS idx_application_status   ON masesas.loan_application (status);

-- ---------------------------------------------------------------------------
-- 6. Transaksi: pembayaran angsuran
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS masesas.loan_payment (
    id                  integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_loan_application integer NOT NULL,
    angsuran_ke         integer NOT NULL,
    jumlah_bayar        numeric(18,2) NOT NULL,
    tanggal_bayar       date NOT NULL,
    metode              varchar(20) NOT NULL,
    created_date        timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_date        timestamp DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_application FOREIGN KEY (id_loan_application)
        REFERENCES masesas.loan_application (id) ON DELETE CASCADE,
    CONSTRAINT uq_payment_angsuran UNIQUE (id_loan_application, angsuran_ke),
    CONSTRAINT ck_payment_jumlah   CHECK (jumlah_bayar > 0),
    CONSTRAINT ck_payment_angsuran CHECK (angsuran_ke > 0)
);

CREATE INDEX IF NOT EXISTS idx_payment_application ON masesas.loan_payment (id_loan_application);

-- ---------------------------------------------------------------------------
-- 7. Seed master
-- ---------------------------------------------------------------------------
INSERT INTO masesas.branch (kode, nama, alamat)
VALUES ('BR01', 'Cabang Jakarta Pusat', 'Jl. Merdeka Selatan No. 1, Jakarta Pusat'),
       ('BR02', 'Cabang Bandung',       'Jl. Asia Afrika No. 8, Bandung')
ON CONFLICT (kode) DO NOTHING;

INSERT INTO masesas.loan_product (kode, nama, bunga_persen, tenor_min, tenor_max, plafond_min, plafond_max)
VALUES ('KTA',  'Kredit Tanpa Agunan',   12.50,  6, 36,  5000000.00,  50000000.00),
       ('KMG',  'Kredit Multiguna',       9.75, 12, 60, 10000000.00, 200000000.00)
ON CONFLICT (kode) DO NOTHING;

INSERT INTO masesas.loan_document_type (kode, nama, wajib)
VALUES ('KTP',       'Kartu Tanda Penduduk', true),
       ('SLIP_GAJI', 'Slip Gaji 3 Bulan Terakhir', true),
       ('NPWP',      'Nomor Pokok Wajib Pajak', false)
ON CONFLICT (kode) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 8. Seed plafond untuk dua customer demo
-- ---------------------------------------------------------------------------
INSERT INTO masesas.loan_plafond (id_customer, plafond_total, plafond_terpakai)
SELECT c.id, v.total, 0
  FROM (VALUES ('customer1@masesas.test', 50000000.00),
               ('customer2@masesas.test', 20000000.00)) AS v(email, total)
  JOIN masesas.customer c ON c.email = v.email
ON CONFLICT (id_customer) DO NOTHING;

COMMIT;

-- ---------------------------------------------------------------------------
-- Pemeriksaan setelah migrasi
-- ---------------------------------------------------------------------------
SELECT 'branch' AS tabel, count(*) AS jumlah FROM masesas.branch
UNION ALL SELECT 'loan_product',       count(*) FROM masesas.loan_product
UNION ALL SELECT 'loan_document_type', count(*) FROM masesas.loan_document_type
UNION ALL SELECT 'loan_plafond',       count(*) FROM masesas.loan_plafond
UNION ALL SELECT 'loan_application',   count(*) FROM masesas.loan_application
UNION ALL SELECT 'loan_payment',       count(*) FROM masesas.loan_payment;

SELECT c.email,
       p.plafond_total,
       p.plafond_terpakai,
       p.plafond_total - p.plafond_terpakai AS sisa
  FROM masesas.loan_plafond p
  JOIN masesas.customer c ON c.id = p.id_customer
 ORDER BY c.email;
