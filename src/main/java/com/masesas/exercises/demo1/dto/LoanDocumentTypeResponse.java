package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.LoanDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Jenis dokumen pinjaman beserta jejak auditnya")
public class LoanDocumentTypeResponse {

    @Schema(description = "ID jenis dokumen", example = "1")
    private Integer id;
    @Schema(description = "Kode jenis dokumen", example = "KTP")
    private String kode;
    @Schema(description = "Nama jenis dokumen", example = "Kartu Tanda Penduduk")
    private String nama;
    @Schema(description = "Wajib dilampirkan", example = "true")
    private Boolean wajib;
    @Schema(description = "Waktu pembuatan baris", example = "2026-08-23T15:04:05Z")
    private Instant createdDate;
    @Schema(description = "Waktu perubahan terakhir", example = "2026-08-23T15:04:05Z")
    private Instant updatedDate;

    public static LoanDocumentTypeResponse from(LoanDocumentType jenis) {
        return new LoanDocumentTypeResponse(
                jenis.getId(),
                jenis.getKode(),
                jenis.getNama(),
                jenis.getWajib(),
                jenis.getCreatedDate(),
                jenis.getUpdatedDate());
    }
}
