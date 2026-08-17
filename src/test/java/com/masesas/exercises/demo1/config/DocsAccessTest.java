package com.masesas.exercises.demo1.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DocsAccessTest {

    private static final String CSP = "Content-Security-Policy";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("halaman dokumentasi terbuka tanpa token")
    void docs_tanpaToken() throws Exception {
        mockMvc.perform(get("/docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Scalar.createApiReference")));
    }

    @Test
    @DisplayName("spesifikasi OpenAPI terbuka tanpa token")
    void openapi_tanpaToken() throws Exception {
        mockMvc.perform(get("/docs/openapi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/karyawan/all']").exists());
    }

    @Test
    @DisplayName("bundle JS Scalar disajikan dari origin yang sama")
    void scalarJs_tanpaToken() throws Exception {
        mockMvc.perform(get("/docs/scalar.js"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("halaman dokumentasi memuat script inline, jadi CSP-nya dilonggarkan")
    void docs_cspLonggar() throws Exception {
        mockMvc.perform(get("/docs"))
                .andExpect(header().string(CSP, containsString("script-src 'self' 'unsafe-inline'")))
                .andExpect(header().string(CSP, containsString("connect-src 'self'")));
    }

    @Test
    @DisplayName("kelonggaran CSP tidak menular ke endpoint API")
    void api_cspTetapKetat() throws Exception {
        mockMvc.perform(get("/api/karyawan/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(CSP, containsString("default-src 'none'")))
                .andExpect(header().string(CSP, not(containsString("unsafe-inline"))));
    }

    @Test
    @DisplayName("kedua security scheme menunjuk ke endpoint token password flow")
    void openapi_securityScheme() throws Exception {
        mockMvc.perform(get("/docs/openapi"))
                .andExpect(jsonPath("$.components.securitySchemes.karyawanAuth.type").value("oauth2"))
                .andExpect(jsonPath("$.components.securitySchemes.karyawanAuth.flows.password.tokenUrl")
                        .value("/api/auth/karyawan/token"))
                .andExpect(jsonPath("$.components.securitySchemes.customerAuth.flows.password.tokenUrl")
                        .value("/api/auth/customer/token"));
    }

    @Test
    @DisplayName("endpoint ber-peran mencantumkan security requirement-nya")
    void openapi_securityRequirement() throws Exception {
        mockMvc.perform(get("/docs/openapi"))
                .andExpect(jsonPath("$.paths['/api/karyawan/all'].get.security[*].karyawanAuth").exists())
                .andExpect(jsonPath("$.paths['/api/customer/me'].get.security[*].customerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/auth/karyawan/login'].post.security").doesNotExist());
    }

    @Test
    @DisplayName("setiap operation punya summary — endpoint baru tidak boleh lolos tanpa dokumentasi")
    void openapi_semuaOperationTerdokumentasi() throws Exception {
        mockMvc.perform(get("/docs/openapi"))
                .andExpect(jsonPath("$.paths..summary").exists())
                .andExpect(jsonPath("$.paths.*.*[?(!@.summary)]").isEmpty());
    }

    @Test
    @DisplayName("kontrak error dari GlobalExceptionHandler melekat ke setiap operation")
    void openapi_kontrakError() throws Exception {
        mockMvc.perform(get("/docs/openapi"))
                .andExpect(jsonPath("$.paths['/api/karyawan/all'].get.responses.401").exists())
                .andExpect(jsonPath("$.paths['/api/karyawan/all'].get.responses.403").exists())
                .andExpect(jsonPath("$.paths['/api/karyawan/all'].get.responses.404.content['application/json'].schema.$ref")
                        .value("#/components/schemas/ApiError"))
                .andExpect(jsonPath("$.components.schemas.ApiError.properties.message.description").isNotEmpty());
    }
}
