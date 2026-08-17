package com.masesas.exercises.demo1.dummyjsondto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Metadata produk seperti dikirim dummyjson.com")
public class ProductMeta {

    @Schema(description = "Waktu pembuatan data di dummyjson.com", example = "2025-04-30T09:41:02.053Z")
    private String createdAt;
    @Schema(description = "Waktu perubahan terakhir di dummyjson.com", example = "2025-04-30T09:41:02.053Z")
    private String updatedAt;
    @Schema(description = "Barcode produk", example = "9164035109868")
    private String barcode;
    @Schema(description = "Alamat gambar QR produk", example = "https://cdn.dummyjson.com/public/qr-code.png")
    private String qrCode;
}
