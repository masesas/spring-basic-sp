package com.masesas.exercises.demo1.dummyjsondto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Produk seperti dikirim dummyjson.com, diteruskan apa adanya")
public class ProductResponse {

    @Schema(description = "ID produk", example = "1")
    private Integer id;
    @Schema(description = "Nama produk", example = "Essence Mascara Lash Princess")
    private String title;
    @Schema(description = "Penjelasan produk", example = "Mascara populer dengan efek bulu mata lentik.")
    private String description;
    @Schema(description = "Kategori produk", example = "beauty")
    private String category;
    @Schema(description = "Harga satuan", example = "9.99")
    private BigDecimal price;
    @Schema(description = "Besar diskon dalam persen", example = "10.48")
    private BigDecimal discountPercentage;
    @Schema(description = "Rata-rata penilaian, skala 0-5", example = "2.56")
    private BigDecimal rating;
    @Schema(description = "Jumlah stok tersedia", example = "99")
    private Integer stock;
    @Schema(description = "Label produk")
    private List<String> tags;
    @Schema(description = "Merek produk", example = "Essence")
    private String brand;
    @Schema(description = "Kode SKU", example = "BEA-ESS-ESS-001")
    private String sku;
    @Schema(description = "Berat produk", example = "4")
    private BigDecimal weight;
    @Schema(description = "Dimensi produk")
    private ProductDimensions dimensions;
    @Schema(description = "Keterangan garansi", example = "1 week warranty")
    private String warrantyInformation;
    @Schema(description = "Keterangan pengiriman", example = "Ships in 3-5 business days")
    private String shippingInformation;
    @Schema(description = "Status ketersediaan", example = "In Stock")
    private String availabilityStatus;
    @Schema(description = "Ulasan dari pembeli")
    private List<ProductReview> reviews;
    @Schema(description = "Kebijakan pengembalian", example = "No return policy")
    private String returnPolicy;
    @Schema(description = "Jumlah pembelian minimum", example = "48")
    private Integer minimumOrderQuantity;
    @Schema(description = "Metadata produk")
    private ProductMeta meta;
    @Schema(description = "Alamat gambar produk")
    private List<String> images;
    @Schema(description = "Alamat gambar kecil produk", example = "https://cdn.dummyjson.com/product-images/1/thumbnail.png")
    private String thumbnail;
}
