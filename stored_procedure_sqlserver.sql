-- ============================================================
--  Stored procedure SQL Server - schema masesas
--  Menggabungkan: variabel, IF, SELECT, UPDATE, INSERT,
--  dan mengembalikan 3 result set sekaligus.
--  Di SQL Server tidak perlu cursor: setiap SELECT otomatis
--  menjadi satu result set.
--  Jalankan lewat SSMS atau: sqlcmd -S <host> -U <user> -P <password> -d <database> -i stored_procedure_sqlserver.sql
-- ============================================================

-- tabel log dipakai oleh bagian INSERT di dalam procedure
IF OBJECT_ID('masesas.log_aktivitas', 'U') IS NULL
BEGIN
    CREATE TABLE masesas.log_aktivitas (
        id           INT IDENTITY(1,1) PRIMARY KEY,
        id_karyawan  INT,
        aktivitas    VARCHAR(250) NOT NULL,
        created_date DATETIME2 DEFAULT SYSUTCDATETIME()
    );
END;
GO

CREATE OR ALTER PROCEDURE masesas.sp_proses_karyawan
    @id     INT,
    @nama   VARCHAR(100) = NULL,
    @alamat VARCHAR(250) = NULL,
    @status VARCHAR(100) = NULL,
    @mode   VARCHAR(20)  = 'RINGKAS'
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @v_nama            VARCHAR(100);
    DECLARE @v_umur            INT;
    DECLARE @v_kategori_umur   VARCHAR(20);
    DECLARE @v_jumlah_rekening INT;
    DECLARE @v_aktivitas       VARCHAR(100);

    -- 1. SELECT INTO variabel: ambil data karyawan
    SELECT @v_nama = k.nama,
           @v_umur = DATEDIFF(YEAR, k.dob, GETDATE())
    FROM masesas.karyawan k
    WHERE k.id = @id AND k.deleted_date IS NULL;

    -- 2. IF: hentikan kalau karyawannya tidak ada
    IF @v_nama IS NULL
    BEGIN
        THROW 50002, 'Karyawan tidak ditemukan.', 1;
    END;

    -- 3. IF bertingkat: tentukan kategori dari variabel umur
    IF @v_umur < 30
        SET @v_kategori_umur = 'MUDA';
    ELSE IF @v_umur < 45
        SET @v_kategori_umur = 'DEWASA';
    ELSE
        SET @v_kategori_umur = 'SENIOR';

    -- 4. IF + UPDATE: baris hanya diubah kalau ada parameter yang diisi
    IF @nama IS NOT NULL OR @alamat IS NOT NULL OR @status IS NOT NULL
    BEGIN
        IF @status IS NOT NULL AND @status NOT IN ('AKTIF', 'NONAKTIF')
        BEGIN
            THROW 50001, 'Status tidak valid. Gunakan AKTIF atau NONAKTIF.', 1;
        END;

        UPDATE masesas.karyawan
        SET nama         = ISNULL(@nama, nama),
            alamat       = ISNULL(@alamat, alamat),
            status       = ISNULL(@status, status),
            updated_date = SYSUTCDATETIME()
        WHERE id = @id;

        SET @v_aktivitas = 'UPDATE data karyawan';
    END
    ELSE
    BEGIN
        SET @v_aktivitas = 'LIHAT data karyawan';
    END;

    -- 5. SELECT INTO variabel: hitung jumlah rekening
    SELECT @v_jumlah_rekening = COUNT(*)
    FROM masesas.rekening
    WHERE id_karyawan = @id AND deleted_date IS NULL;

    -- 6. INSERT: catat aktivitas ke tabel log
    INSERT INTO masesas.log_aktivitas (id_karyawan, aktivitas)
    VALUES (@id, @v_aktivitas + ' | ' + @v_kategori_umur
                 + ' | rekening: ' + CAST(@v_jumlah_rekening AS VARCHAR(10)));

    -- 7. RESULT SET 1: data karyawan + detail karyawan
    SELECT k.id,
           k.nama,
           k.alamat,
           k.status,
           k.dob,
           @v_umur            AS umur,
           @v_kategori_umur   AS kategori_umur,
           d.nik,
           d.npwp,
           @v_jumlah_rekening AS jumlah_rekening
    FROM masesas.karyawan k
             LEFT JOIN masesas.detail_karyawan d
                       ON d.id = k.detail_karyawan AND d.deleted_date IS NULL
    WHERE k.id = @id;

    -- 8. RESULT SET 2: daftar rekening milik karyawan
    SELECT r.id, r.nama, r.jenis, r.rekening
    FROM masesas.rekening r
    WHERE r.id_karyawan = @id AND r.deleted_date IS NULL
    ORDER BY r.id;

    -- 9. RESULT SET 3: IF berdasarkan parameter @mode
    IF UPPER(@mode) = 'LENGKAP'
    BEGIN
        -- mode LENGKAP: semua training yang pernah diikuti
        SELECT t.id, t.tema, t.pengajar, kt.tanggal
        FROM masesas.karyawan_training kt
                 JOIN masesas.training t ON t.id = kt.id_training AND t.deleted_date IS NULL
        WHERE kt.id_karyawan = @id AND kt.deleted_date IS NULL
        ORDER BY kt.tanggal DESC;
    END
    ELSE
    BEGIN
        -- mode RINGKAS: cukup training terakhir
        SELECT TOP 1 t.id, t.tema, t.pengajar, kt.tanggal
        FROM masesas.karyawan_training kt
                 JOIN masesas.training t ON t.id = kt.id_training AND t.deleted_date IS NULL
        WHERE kt.id_karyawan = @id AND kt.deleted_date IS NULL
        ORDER BY kt.tanggal DESC;
    END;
END;
GO

-- ============================================================
--  Contoh pemakaian
-- ============================================================
-- EXEC masesas.sp_proses_karyawan @id = 7, @mode = 'LENGKAP';
-- EXEC masesas.sp_proses_karyawan @id = 7, @nama = 'Budi Santoso', @status = 'AKTIF';
