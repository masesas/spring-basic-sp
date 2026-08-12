package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.config.prop.AppConfigProperties;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DummyJsonServiceTest {

    private static final String BASE_URL = "https://dummyjson.com";

    private static final String JSON_PENCARIAN = """
            {
              "products": [
                {
                  "id": 121,
                  "title": "iPhone 5s",
                  "category": "smartphones",
                  "price": 199.99,
                  "rating": 4.02,
                  "stock": 51,
                  "tags": ["smartphones"],
                  "brand": "Apple",
                  "dimensions": {"width": 12.35, "height": 24.15, "depth": 25.61},
                  "reviews": [
                    {
                      "rating": 4,
                      "comment": "Very satisfied!",
                      "date": "2025-04-30T09:41:02.053Z",
                      "reviewerName": "Zoey Reed",
                      "reviewerEmail": "zoey.reed@x.dummyjson.com"
                    }
                  ],
                  "meta": {"barcode": "1889363886067", "qrCode": "https://cdn.dummyjson.com/public/qr-code.png"},
                  "images": ["https://cdn.dummyjson.com/products/images/smartphones/iPhone%205s/1.png"],
                  "thumbnail": "https://cdn.dummyjson.com/products/images/smartphones/iPhone%205s/thumbnail.png"
                }
              ],
              "total": 1,
              "skip": 0,
              "limit": 1
            }
            """;

    private static final String JSON_PRODUK = """
            {
              "id": 1,
              "title": "Essence Mascara Lash Princess",
              "description": "The Essence Mascara Lash Princess is a popular mascara.",
              "category": "beauty",
              "price": 9.99,
              "discountPercentage": 10.48,
              "rating": 2.56,
              "stock": 99,
              "tags": ["beauty", "mascara"],
              "brand": "Essence",
              "sku": "BEA-ESS-ESS-001",
              "weight": 4,
              "dimensions": {"width": 15.14, "height": 13.08, "depth": 22.99},
              "warrantyInformation": "1 week warranty",
              "shippingInformation": "Ships in 3-5 business days",
              "availabilityStatus": "In Stock",
              "reviews": [],
              "returnPolicy": "No return policy",
              "minimumOrderQuantity": 48,
              "meta": {
                "createdAt": "2025-04-30T09:41:02.053Z",
                "updatedAt": "2025-04-30T09:41:02.053Z",
                "barcode": "5784719087687",
                "qrCode": "https://cdn.dummyjson.com/public/qr-code.png"
              },
              "images": ["https://cdn.dummyjson.com/product-images/beauty/1/1.webp"],
              "thumbnail": "https://cdn.dummyjson.com/product-images/beauty/1/thumbnail.webp"
            }
            """;

    private MockRestServiceServer server;
    private DummyJsonService dummyJsonService;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);

        AppConfigProperties.DummyJson dummyJson = new AppConfigProperties.DummyJson();
        dummyJson.setBaseUrl(BASE_URL);
        AppConfigProperties properties = new AppConfigProperties();
        properties.setDummyJson(dummyJson);

        dummyJsonService = new DummyJsonService(restTemplate, properties);
    }

    @Test
    @DisplayName("cariProduk mengirim query q dan memetakan hasil pencarian")
    void cariProduk_denganQuery_memetakanRespons() {
        server.expect(requestTo(BASE_URL + "/products/search?q=phone"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(JSON_PENCARIAN, MediaType.APPLICATION_JSON));

        var hasil = dummyJsonService.cariProduk("phone");

        server.verify();
        assertThat(hasil.getTotal()).isEqualTo(1);
        assertThat(hasil.getSkip()).isZero();
        assertThat(hasil.getLimit()).isEqualTo(1);
        assertThat(hasil.getProducts()).hasSize(1);

        var produk = hasil.getProducts().get(0);
        assertThat(produk.getId()).isEqualTo(121);
        assertThat(produk.getTitle()).isEqualTo("iPhone 5s");
        assertThat(produk.getPrice()).isEqualByComparingTo(new BigDecimal("199.99"));
        assertThat(produk.getTags()).containsExactly("smartphones");
        assertThat(produk.getDimensions().getWidth()).isEqualByComparingTo(new BigDecimal("12.35"));
        assertThat(produk.getReviews()).hasSize(1);
        assertThat(produk.getReviews().get(0).getReviewerName()).isEqualTo("Zoey Reed");
        assertThat(produk.getMeta().getBarcode()).isEqualTo("1889363886067");
    }

    @Test
    @DisplayName("cariProduk dengan query kosong tidak mengirim parameter q")
    void cariProduk_tanpaQuery_tidakMengirimParameter() {
        server.expect(requestTo(BASE_URL + "/products/search"))
                .andRespond(withSuccess(JSON_PENCARIAN, MediaType.APPLICATION_JSON));

        var hasil = dummyJsonService.cariProduk("   ");

        server.verify();
        assertThat(hasil.getProducts()).hasSize(1);
    }

    @Test
    @DisplayName("cariProduk meng-encode spasi pada query")
    void cariProduk_queryBerspasi_diEncode() {
        server.expect(requestTo(BASE_URL + "/products/search?q=phone%20case"))
                .andRespond(withSuccess(JSON_PENCARIAN, MediaType.APPLICATION_JSON));

        dummyJsonService.cariProduk("phone case");

        server.verify();
    }

    @Test
    @DisplayName("ambilProduk memetakan seluruh field produk")
    void ambilProduk_idAda_memetakanRespons() {
        server.expect(requestTo(BASE_URL + "/products/1"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(JSON_PRODUK, MediaType.APPLICATION_JSON));

        var produk = dummyJsonService.ambilProduk(1);

        server.verify();
        assertThat(produk.getId()).isEqualTo(1);
        assertThat(produk.getTitle()).isEqualTo("Essence Mascara Lash Princess");
        assertThat(produk.getCategory()).isEqualTo("beauty");
        assertThat(produk.getPrice()).isEqualByComparingTo(new BigDecimal("9.99"));
        assertThat(produk.getDiscountPercentage()).isEqualByComparingTo(new BigDecimal("10.48"));
        assertThat(produk.getStock()).isEqualTo(99);
        assertThat(produk.getSku()).isEqualTo("BEA-ESS-ESS-001");
        assertThat(produk.getAvailabilityStatus()).isEqualTo("In Stock");
        assertThat(produk.getMinimumOrderQuantity()).isEqualTo(48);
        assertThat(produk.getMeta().getCreatedAt()).isEqualTo("2025-04-30T09:41:02.053Z");
        assertThat(produk.getImages()).hasSize(1);
        assertThat(produk.getThumbnail()).endsWith("thumbnail.webp");
    }

    @Test
    @DisplayName("ambilProduk melempar ResourceNotFoundException saat upstream 404")
    void ambilProduk_idTidakAda_melemparNotFound() {
        server.expect(requestTo(BASE_URL + "/products/999999"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Product with id '999999' not found\"}"));

        assertThatThrownBy(() -> dummyJsonService.ambilProduk(999999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999999");

        server.verify();
    }
}
