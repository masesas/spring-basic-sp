-- ============================================================================
--  masesas.payroll_karyawan
--  Tabel slip gaji dengan COMPOSITE PRIMARY KEY (id_karyawan, periode).
--
--  Tabel ini sengaja dibuat tanpa surrogate id dan tanpa sequence: satu slip
--  gaji secara alami diidentifikasi oleh "karyawan mana, periode apa".
--  Di sisi Java, PK ini dipetakan dengan @EmbeddedId + @MapsId.
-- ============================================================================

CREATE TABLE IF NOT EXISTS payroll_karyawan (
    -- Dua kolom ini berperan ganda: bagian dari PK sekaligus foreign key.
    -- @MapsId di sisi JPA yang menjaga agar tidak terduplikasi jadi kolom terpisah.
    id_karyawan   integer       NOT NULL,
    periode       date          NOT NULL,

    -- Komponen gaji — dipetakan sebagai satu @Embeddable KomponenGaji.
    gaji_pokok    numeric(15,2) NOT NULL DEFAULT 0,
    tunjangan     numeric(15,2) NOT NULL DEFAULT 0,
    potongan      numeric(15,2) NOT NULL DEFAULT 0,

    -- Tidak ada deleted_date: slip gaji adalah catatan finansial.
    -- Kesalahan diperbaiki lewat UPDATE, bukan dihapus lalu dibuat ulang.
    -- Soft delete juga akan bertabrakan dengan composite PK.
    created_date  timestamp,
    updated_date  timestamp,

    CONSTRAINT payroll_karyawan_pkey
        PRIMARY KEY (id_karyawan, periode),

    CONSTRAINT payroll_karyawan_id_karyawan_fkey
        FOREIGN KEY (id_karyawan) REFERENCES masesas.karyawan (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
);

-- Index PK adalah B-tree pada (id_karyawan, periode), sehingga hanya efisien
-- untuk filter yang diawali id_karyawan. Query lintas karyawan dalam satu
-- periode ("semua slip gaji Agustus 2026") butuh index sendiri.
CREATE INDEX IF NOT EXISTS idx_payroll_karyawan_periode
    ON masesas.payroll_karyawan (periode);

COMMENT ON TABLE  masesas.payroll_karyawan IS 'Slip gaji per karyawan per periode bulanan';
COMMENT ON COLUMN masesas.payroll_karyawan.periode IS 'Tanggal 1 dari bulan periode gaji';
