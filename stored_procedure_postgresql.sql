-- ============================================================
--  Stored procedure PostgreSQL - schema masesas
--  Menggabungkan: variabel, IF, SELECT, UPDATE, INSERT,
--  dan mengembalikan 3 result set sekaligus lewat refcursor.
--  Jalankan: psql -h 129.226.195.9 -p 5432 -U binar_admin -d binar_finance -f stored_procedure_postgresql.sql
-- ============================================================

SET search_path = masesas;

-- tabel log dipakai oleh bagian INSERT di dalam procedure
CREATE TABLE IF NOT EXISTS masesas.log_aktivitas (
    id           SERIAL PRIMARY KEY,
    id_karyawan  INTEGER,
    aktivitas    VARCHAR(250) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE PROCEDURE masesas.sp_proses_karyawan(
    p_id     INTEGER,
    p_nama   VARCHAR   DEFAULT NULL,
    p_alamat VARCHAR   DEFAULT NULL,
    p_status VARCHAR   DEFAULT NULL,
    p_mode   VARCHAR   DEFAULT 'RINGKAS',
    -- tiga cursor di bawah ini adalah tiga result set yang dikembalikan
    INOUT p_karyawan refcursor DEFAULT 'cur_karyawan',
    INOUT p_rekening refcursor DEFAULT 'cur_rekening',
    INOUT p_training refcursor DEFAULT 'cur_training'
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_nama            VARCHAR;
    v_umur            INTEGER;
    v_kategori_umur   VARCHAR;
    v_jumlah_rekening INTEGER;
    v_aktivitas       VARCHAR;
BEGIN
    -- 1. SELECT INTO: isi variabel dari tabel karyawan
    SELECT k.nama, EXTRACT(YEAR FROM age(k.dob))::INTEGER
    INTO v_nama, v_umur
    FROM masesas.karyawan k
    WHERE k.id = p_id AND k.deleted_date IS NULL;

    -- 2. IF: hentikan kalau karyawannya tidak ada
    IF v_nama IS NULL THEN
        RAISE EXCEPTION 'Karyawan dengan id % tidak ditemukan.', p_id;
    END IF;

    -- 3. IF bertingkat: tentukan kategori dari variabel umur
    IF v_umur < 30 THEN
        v_kategori_umur := 'MUDA';
    ELSIF v_umur < 45 THEN
        v_kategori_umur := 'DEWASA';
    ELSE
        v_kategori_umur := 'SENIOR';
    END IF;

    -- 4. IF + UPDATE: baris hanya diubah kalau ada parameter yang diisi
    IF p_nama IS NOT NULL OR p_alamat IS NOT NULL OR p_status IS NOT NULL THEN

        IF p_status IS NOT NULL AND p_status NOT IN ('AKTIF', 'NONAKTIF') THEN
            RAISE EXCEPTION 'Status tidak valid: %. Gunakan AKTIF atau NONAKTIF.', p_status;
        END IF;

        UPDATE masesas.karyawan
        SET nama         = COALESCE(p_nama, nama),
            alamat       = COALESCE(p_alamat, alamat),
            status       = COALESCE(p_status, status),
            updated_date = now()
        WHERE id = p_id;

        v_aktivitas := 'UPDATE data karyawan';
    ELSE
        v_aktivitas := 'LIHAT data karyawan';
    END IF;

    -- 5. SELECT INTO: hitung jumlah rekening ke variabel
    SELECT COUNT(*) INTO v_jumlah_rekening
    FROM masesas.rekening
    WHERE id_karyawan = p_id AND deleted_date IS NULL;

    -- 6. INSERT: catat aktivitas ke tabel log
    INSERT INTO masesas.log_aktivitas (id_karyawan, aktivitas)
    VALUES (p_id, v_aktivitas || ' | ' || v_kategori_umur || ' | rekening: ' || v_jumlah_rekening);

    -- 7. RESULT SET 1: data karyawan + detail karyawan
    OPEN p_karyawan FOR
        SELECT k.id,
               k.nama,
               k.alamat,
               k.status,
               k.dob,
               v_umur            AS umur,
               v_kategori_umur   AS kategori_umur,
               d.nik,
               d.npwp,
               v_jumlah_rekening AS jumlah_rekening
        FROM masesas.karyawan k
                 LEFT JOIN masesas.detail_karyawan d
                           ON d.id = k.detail_karyawan AND d.deleted_date IS NULL
        WHERE k.id = p_id;

    -- 8. RESULT SET 2: daftar rekening milik karyawan
    OPEN p_rekening FOR
        SELECT r.id, r.nama, r.jenis, r.rekening
        FROM masesas.rekening r
        WHERE r.id_karyawan = p_id AND r.deleted_date IS NULL
        ORDER BY r.id;

    -- 9. RESULT SET 3: IF berdasarkan parameter p_mode
    IF upper(p_mode) = 'LENGKAP' THEN
        -- mode LENGKAP: semua training yang pernah diikuti
        OPEN p_training FOR
            SELECT t.id, t.tema, t.pengajar, kt.tanggal
            FROM masesas.karyawan_training kt
                     JOIN masesas.training t ON t.id = kt.id_training AND t.deleted_date IS NULL
            WHERE kt.id_karyawan = p_id AND kt.deleted_date IS NULL
            ORDER BY kt.tanggal DESC;
    ELSE
        -- mode RINGKAS: cukup training terakhir
        OPEN p_training FOR
            SELECT t.id, t.tema, t.pengajar, kt.tanggal
            FROM masesas.karyawan_training kt
                     JOIN masesas.training t ON t.id = kt.id_training AND t.deleted_date IS NULL
            WHERE kt.id_karyawan = p_id AND kt.deleted_date IS NULL
            ORDER BY kt.tanggal DESC
            LIMIT 1;
    END IF;
END;
$$;

-- ============================================================
--  Contoh pemakaian
--  Wajib di dalam transaksi: cursor otomatis tertutup saat COMMIT.
-- ============================================================
-- BEGIN;
-- CALL masesas.sp_proses_karyawan(7, NULL, NULL, NULL, 'LENGKAP', 'c1', 'c2', 'c3');
-- FETCH ALL IN c1;   -- data karyawan
-- FETCH ALL IN c2;   -- daftar rekening
-- FETCH ALL IN c3;   -- daftar training
-- COMMIT;
--
-- BEGIN;
-- CALL masesas.sp_proses_karyawan(7, 'Budi Santoso', NULL, 'AKTIF', 'RINGKAS', 'c1', 'c2', 'c3');
-- FETCH ALL IN c1;
-- COMMIT;
