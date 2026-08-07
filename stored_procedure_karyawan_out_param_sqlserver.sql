-- ============================================================
--  Stored procedure SQL Server dengan OUTPUT parameter - schema masesas
--  Versi T-SQL dari stored_procedure_karyawan_out_param.sql.
--  Dipakai oleh @NamedStoredProcedureQuery yang sama pada entity Karyawan
--  (kode Java tidak perlu diubah, hanya SP-nya yang beda sintaks).
--  Jalankan lewat SSMS atau: sqlcmd -S <host> -U <user> -P <password> -d <database> -i stored_procedure_karyawan_out_param_sqlserver.sql
-- ============================================================

-- ------------------------------------------------------------
--  1. Satu OUTPUT parameter: hasilnya cukup satu angka
-- ------------------------------------------------------------
CREATE OR ALTER PROCEDURE masesas.sp_total_karyawan_by_status
    @status_in VARCHAR(100),
    @total_out INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT @total_out = COUNT(*)
    FROM masesas.karyawan
    WHERE deleted_date IS NULL
      AND status = @status_in;
END;
GO

-- ------------------------------------------------------------
--  2. Beberapa OUTPUT parameter: statistik umur karyawan
-- ------------------------------------------------------------
CREATE OR ALTER PROCEDURE masesas.sp_statistik_karyawan_by_status
    @status_in         VARCHAR(100),
    @total_out         INT OUTPUT,
    @umur_rata_out     DECIMAL(10, 2) OUTPUT,
    @umur_minimum_out  INT OUTPUT,
    @umur_maksimum_out INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    -- umur dihitung dari kolom dob, lalu diambil count/avg/min/max sekaligus
    SELECT @total_out         = COUNT(*),
           @umur_rata_out     = ROUND(AVG(CAST(DATEDIFF(YEAR, dob, GETDATE()) AS DECIMAL(10, 2))), 2),
           @umur_minimum_out  = MIN(DATEDIFF(YEAR, dob, GETDATE())),
           @umur_maksimum_out = MAX(DATEDIFF(YEAR, dob, GETDATE()))
    FROM masesas.karyawan
    WHERE deleted_date IS NULL
      AND status = @status_in;
END;
GO

-- ============================================================
--  Contoh pemakaian
-- ============================================================
-- DECLARE @total INT;
-- EXEC masesas.sp_total_karyawan_by_status @status_in = 'AKTIF', @total_out = @total OUTPUT;
-- SELECT @total AS total;
--
-- DECLARE @t INT, @rata DECIMAL(10,2), @min INT, @max INT;
-- EXEC masesas.sp_statistik_karyawan_by_status 'AKTIF', @t OUTPUT, @rata OUTPUT, @min OUTPUT, @max OUTPUT;
-- SELECT @t AS total, @rata AS umur_rata, @min AS umur_minimum, @max AS umur_maksimum;

-- ============================================================
--  Catatan untuk sisi Java
--  Nama & urutan parameter di @NamedStoredProcedureQuery harus sama
--  dengan procedure di atas, karena driver SQL Server mengikat
--  parameter berdasarkan urutan (posisi), bukan nama.
-- ============================================================
