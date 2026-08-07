-- ============================================================
--  Stored procedure PostgreSQL dengan OUT parameter - schema masesas
--  Dipakai oleh @NamedStoredProcedureQuery pada entity Karyawan
--  dan @Procedure pada KaryawanStatistikRepository.
--  Jalankan: psql -h 129.226.195.9 -p 5432 -U binar_admin -d binar_finance -f stored_procedure_karyawan_out_param.sql
-- ============================================================

SET search_path = masesas;

-- ------------------------------------------------------------
--  1. Satu OUT parameter: hasilnya cukup satu angka
-- ------------------------------------------------------------
CREATE OR REPLACE PROCEDURE masesas.sp_total_karyawan_by_status(
    IN  status_in VARCHAR,
    OUT total_out INTEGER
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT COUNT(*)
    INTO total_out
    FROM masesas.karyawan
    WHERE deleted_date IS NULL
      AND status = status_in;
END;
$$;

-- ------------------------------------------------------------
--  2. Beberapa OUT parameter: statistik umur karyawan
-- ------------------------------------------------------------
CREATE OR REPLACE PROCEDURE masesas.sp_statistik_karyawan_by_status(
    IN  status_in        VARCHAR,
    OUT total_out        INTEGER,
    OUT umur_rata_out    NUMERIC,
    OUT umur_minimum_out INTEGER,
    OUT umur_maksimum_out INTEGER
)
LANGUAGE plpgsql
AS $$
BEGIN
    -- umur dihitung dari kolom dob, lalu diambil count/avg/min/max sekaligus
    SELECT COUNT(*),
           ROUND(AVG(EXTRACT(YEAR FROM age(dob))), 2),
           MIN(EXTRACT(YEAR FROM age(dob)))::INTEGER,
           MAX(EXTRACT(YEAR FROM age(dob)))::INTEGER
    INTO total_out, umur_rata_out, umur_minimum_out, umur_maksimum_out
    FROM masesas.karyawan
    WHERE deleted_date IS NULL
      AND status = status_in;
END;
$$;

-- ============================================================
--  Contoh pemakaian
-- ============================================================
-- CALL masesas.sp_total_karyawan_by_status('AKTIF', NULL);
-- CALL masesas.sp_statistik_karyawan_by_status('AKTIF', NULL, NULL, NULL, NULL);
