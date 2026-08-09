-- ============================================================================
--  masesas.payroll_karyawan — tambah kolom status (OWASP A04 Insecure Design)
--
--  Slip gaji yang sudah disetujui tidak boleh direvisi diam-diam. Tanpa kolom
--  ini tidak ada cara membedakan slip yang masih draft dari slip yang sudah
--  jadi dasar pembayaran.
--
--  Aman dijalankan berulang kali. Bersifat menambah saja: baris lama otomatis
--  berstatus DRAFT sehingga tidak ada data lama yang menjadi tidak valid.
-- ============================================================================

ALTER TABLE masesas.payroll_karyawan
    ADD COLUMN IF NOT EXISTS status varchar(16) NOT NULL DEFAULT 'DRAFT';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'payroll_karyawan_status_check'
          AND conrelid = 'masesas.payroll_karyawan'::regclass
    ) THEN
        ALTER TABLE masesas.payroll_karyawan
            ADD CONSTRAINT payroll_karyawan_status_check
            CHECK (status IN ('DRAFT', 'APPROVED'));
    END IF;
END $$;
