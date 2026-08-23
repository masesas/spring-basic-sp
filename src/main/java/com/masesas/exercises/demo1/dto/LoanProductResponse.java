package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.LoanProduct;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Produk pinjaman beserta jejak auditnya")
public class LoanProductResponse {

    @Schema(description = "ID produk", example = "1")
    private Integer id;
    @Schema(description = "Kode produk", example = "KTA")
    private String kode;
    @Schema(description = "Nama produk", example = "Kredit Tanpa Agunan")
    private String nama;
    @Schema(description = "Bunga per tahun dalam persen", example = "12.50")
    private BigDecimal bungaPersen;
    @Schema(description = "Tenor terpendek dalam bulan", example = "6")
    private Integer tenorMin;
    @Schema(description = "Tenor terpanjang dalam bulan", example = "36")
    private Integer tenorMax;
    @Schema(description = "Nominal pinjaman terkecil", example = "5000000.00")
    private BigDecimal plafondMin;
    @Schema(description = "Nominal pinjaman terbesar", example = "50000000.00")
    private BigDecimal plafondMax;
    @Schema(description = "Produk bisa dipakai mengajukan pinjaman", example = "true")
    private Boolean aktif;
    @Schema(description = "Waktu pembuatan baris", example = "2026-08-23T15:04:05Z")
    private Instant createdDate;
    @Schema(description = "Waktu perubahan terakhir", example = "2026-08-23T15:04:05Z")
    private Instant updatedDate;

    public static LoanProductResponse from(LoanProduct produk) {
        return new LoanProductResponse(
                produk.getId(),
                produk.getKode(),
                produk.getNama(),
                produk.getBungaPersen(),
                produk.getTenorMin(),
                produk.getTenorMax(),
                produk.getPlafondMin(),
                produk.getPlafondMax(),
                produk.getAktif(),
                produk.getCreatedDate(),
                produk.getUpdatedDate());
    }
}
