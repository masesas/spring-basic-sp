-- ============================================================================
--  seed_customer_loan_masesas.sql — data contoh customer dan modul pinjaman
-- ============================================================================
--
--  Isi: 10 customer, 200 pengajuan pinjaman, 500 pembayaran angsuran, dan
--  plafond kredit untuk kesepuluh customer tersebut.
--
--  Cara pakai:
--
--      HASH="{bcrypt}$(htpasswd -bnBC 12 "" "$DEMO_PASSWORD" | tr -d ':\n')"
--      psql -h <host> -p 5432 -U <user> -d binar_finance \
--           -v pwd_hash="$HASH" -f seed_customer_loan_masesas.sql
--
--  Hash password TIDAK ditulis di berkas ini, sama seperti rbac_masesas.sql:
--  nilainya dipasok lewat variabel psql :'pwd_hash' saat eksekusi.
--
--  Jalankan SETELAH rbac_masesas.sql dan loan_masesas.sql — berkas ini
--  bergantung pada tabel customer, branch, loan_product, loan_plafond,
--  loan_application, dan loan_payment yang dibuat di sana.
--
--  Aman dijalankan berulang kali:
--    * customer memakai ON CONFLICT (email) DO NOTHING
--    * loan_application tidak punya kunci alami, jadi dijaga NOT EXISTS —
--      begitu satu pengajuan milik customer seed sudah ada, insert kedua
--      tidak menghasilkan baris apa pun
--    * loan_payment memakai ON CONFLICT (id_loan_application, angsuran_ke)
--    * loan_plafond memakai ON CONFLICT (id_customer)
--
--  Susunan datanya sengaja deterministik supaya hasilnya sama di mesin mana
--  pun dan gampang dipakai sebagai acuan saat latihan:
--
--    * Setiap customer mendapat 20 pengajuan berurutan, satu untuk tiap sisa
--      bagi i modulo 20. Jadi tiap customer punya 5 DISBURSED, 5 DRAFT,
--      5 SUBMITTED, 2 APPROVED, 2 REJECTED, dan 1 CANCELLED.
--    * Hanya pengajuan DISBURSED yang punya angsuran — 50 pengajuan
--      DISBURSED dikali 10 angsuran menghasilkan tepat 500 pembayaran.
--    * Tenor selalu minimal 12 bulan supaya 10 angsuran itu tidak pernah
--      melebihi tenornya sendiri.
--    * jumlah_pengajuan dan tenor selalu berada di dalam rentang plafond dan
--      tenor produknya, sama seperti yang divalidasi LoanApplicationService.
--    * plafond_terpakai dihitung dari jumlah pengajuan DISBURSED, sehingga
--      cocok dengan invarian yang dijaga entity LoanPlafond.
-- ============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. Sepuluh customer contoh
--
--    Email berawalan customer.seed supaya bisa dibedakan dari customer1 dan
--    customer2 bawaan rbac_masesas.sql, yang sengaja tidak disentuh berkas ini.
-- ---------------------------------------------------------------------------
INSERT INTO masesas.customer (nama, email, password, created_date, updated_date)
SELECT v.nama,
       'customer.seed' || lpad(v.n::text, 2, '0') || '@masesas.test',
       :'pwd_hash',
       timestamp '2025-01-01 08:00:00' + make_interval(days => v.n),
       timestamp '2025-01-01 08:00:00' + make_interval(days => v.n)
  FROM (VALUES (1,  'Siti Rahma'),
               (2,  'Budi Santoso'),
               (3,  'Dewi Lestari'),
               (4,  'Agus Prasetyo'),
               (5,  'Rina Kartika'),
               (6,  'Hendra Wijaya'),
               (7,  'Maya Anggraini'),
               (8,  'Fajar Nugroho'),
               (9,  'Putri Ramadhani'),
               (10, 'Iwan Setiawan')) AS v(n, nama)
ON CONFLICT (email) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Dua ratus pengajuan pinjaman
-- ---------------------------------------------------------------------------
WITH seed_customer AS (
    SELECT c.id,
           row_number() OVER (ORDER BY c.email) AS n
      FROM masesas.customer c
     WHERE c.email LIKE 'customer.seed__@masesas.test'
),
pengajuan AS (
    SELECT i,
           CASE WHEN i % 2 = 1 THEN 'KTA'  ELSE 'KMG'  END AS kode_produk,
           CASE WHEN i % 2 = 1 THEN 'BR01' ELSE 'BR02' END AS kode_branch,
           CASE WHEN i % 2 = 1
                THEN  5000000.00 + (i % 10) *  4000000.00
                ELSE 10000000.00 + (i % 10) * 15000000.00
           END::numeric(18, 2) AS jumlah_pengajuan,
           CASE WHEN i % 2 = 1 THEN 12 + (i % 25) ELSE 12 + (i % 49) END AS tenor_bulan,
           CASE i % 20
                WHEN 0  THEN 'DISBURSED' WHEN 4  THEN 'DISBURSED'
                WHEN 8  THEN 'DISBURSED' WHEN 12 THEN 'DISBURSED'
                WHEN 16 THEN 'DISBURSED'
                WHEN 1  THEN 'DRAFT'     WHEN 5  THEN 'DRAFT'
                WHEN 9  THEN 'DRAFT'     WHEN 13 THEN 'DRAFT'
                WHEN 17 THEN 'DRAFT'
                WHEN 2  THEN 'SUBMITTED' WHEN 6  THEN 'SUBMITTED'
                WHEN 10 THEN 'SUBMITTED' WHEN 14 THEN 'SUBMITTED'
                WHEN 18 THEN 'SUBMITTED'
                WHEN 3  THEN 'APPROVED'  WHEN 15 THEN 'APPROVED'
                WHEN 7  THEN 'REJECTED'  WHEN 19 THEN 'REJECTED'
                ELSE 'CANCELLED'
           END AS status,
           timestamp '2025-01-02 09:00:00' + make_interval(days => i) AS ts
      FROM generate_series(1, 200) AS i
)
INSERT INTO masesas.loan_application (id_customer, id_loan_product, id_branch,
                                      jumlah_pengajuan, tenor_bulan, status, catatan,
                                      version, created_date, updated_date)
SELECT sc.id,
       pr.id,
       br.id,
       p.jumlah_pengajuan,
       p.tenor_bulan,
       p.status,
       CASE p.status
            WHEN 'APPROVED'  THEN 'Disetujui, data contoh'
            WHEN 'DISBURSED' THEN 'Disetujui lalu dicairkan, data contoh'
            WHEN 'REJECTED'  THEN 'Ditolak, dokumen pendukung tidak lengkap'
            ELSE NULL
       END,
       0,
       p.ts,
       p.ts
  FROM pengajuan p
  JOIN seed_customer sc          ON sc.n = ((p.i - 1) / 20) + 1
  JOIN masesas.loan_product pr   ON pr.kode = p.kode_produk
  JOIN masesas.branch br         ON br.kode = p.kode_branch
 WHERE NOT EXISTS (
           SELECT 1
             FROM masesas.loan_application la
             JOIN seed_customer sc2 ON sc2.id = la.id_customer);

-- ---------------------------------------------------------------------------
-- 3. Lima ratus pembayaran angsuran
--
--    Besar angsuran memakai bunga flat: pokok ditambah bunga produk, dibagi
--    rata sepanjang tenor. Cukup untuk data latihan, dan selalu lebih besar
--    dari nol sebagaimana disyaratkan ck_payment_jumlah.
-- ---------------------------------------------------------------------------
WITH seed_customer AS (
    SELECT c.id
      FROM masesas.customer c
     WHERE c.email LIKE 'customer.seed__@masesas.test'
),
dicairkan AS (
    SELECT la.id,
           la.jumlah_pengajuan,
           la.tenor_bulan,
           la.created_date,
           pr.bunga_persen
      FROM masesas.loan_application la
      JOIN seed_customer sc        ON sc.id = la.id_customer
      JOIN masesas.loan_product pr ON pr.id = la.id_loan_product
     WHERE la.status = 'DISBURSED'
)
INSERT INTO masesas.loan_payment (id_loan_application, angsuran_ke, jumlah_bayar,
                                  tanggal_bayar, metode, created_date, updated_date)
SELECT d.id,
       n,
       round(d.jumlah_pengajuan * (1 + d.bunga_persen / 100) / d.tenor_bulan, 2),
       (d.created_date + make_interval(months => n))::date,
       (ARRAY['TRANSFER', 'TUNAI', 'VIRTUAL_ACCOUNT'])[1 + ((d.id + n) % 3)],
       d.created_date + make_interval(months => n),
       d.created_date + make_interval(months => n)
  FROM dicairkan d
  CROSS JOIN generate_series(1, 10) AS n
ON CONFLICT (id_loan_application, angsuran_ke) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. Plafond kredit kesepuluh customer
--
--    plafond_terpakai diisi dari total pengajuan yang sudah DISBURSED supaya
--    angkanya konsisten dengan yang dihitung ulang aplikasi.
-- ---------------------------------------------------------------------------
WITH seed_customer AS (
    SELECT c.id
      FROM masesas.customer c
     WHERE c.email LIKE 'customer.seed__@masesas.test'
)
INSERT INTO masesas.loan_plafond (id_customer, plafond_total, plafond_terpakai,
                                  version, created_date, updated_date)
SELECT sc.id,
       1000000000.00,
       coalesce(sum(la.jumlah_pengajuan) FILTER (WHERE la.status = 'DISBURSED'), 0),
       0,
       timestamp '2025-01-01 08:00:00',
       timestamp '2025-01-01 08:00:00'
  FROM seed_customer sc
  LEFT JOIN masesas.loan_application la ON la.id_customer = sc.id
 GROUP BY sc.id
ON CONFLICT (id_customer) DO NOTHING;

COMMIT;

-- ---------------------------------------------------------------------------
-- Pemeriksaan setelah seeder dijalankan
-- ---------------------------------------------------------------------------
SELECT 'customer seed' AS bagian, count(*) AS jumlah
  FROM masesas.customer WHERE email LIKE 'customer.seed__@masesas.test'
UNION ALL
SELECT 'loan_application seed', count(*)
  FROM masesas.loan_application la
  JOIN masesas.customer c ON c.id = la.id_customer
 WHERE c.email LIKE 'customer.seed__@masesas.test'
UNION ALL
SELECT 'loan_payment seed', count(*)
  FROM masesas.loan_payment lp
  JOIN masesas.loan_application la ON la.id = lp.id_loan_application
  JOIN masesas.customer c ON c.id = la.id_customer
 WHERE c.email LIKE 'customer.seed__@masesas.test';

SELECT la.status, count(*) AS jumlah
  FROM masesas.loan_application la
  JOIN masesas.customer c ON c.id = la.id_customer
 WHERE c.email LIKE 'customer.seed__@masesas.test'
 GROUP BY la.status
 ORDER BY la.status;

SELECT c.email,
       p.plafond_total,
       p.plafond_terpakai,
       p.plafond_total - p.plafond_terpakai AS sisa
  FROM masesas.loan_plafond p
  JOIN masesas.customer c ON c.id = p.id_customer
 WHERE c.email LIKE 'customer.seed__@masesas.test'
 ORDER BY c.email;
