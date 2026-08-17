package com.masesas.exercises.demo1.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "demo1 API",
                version = "0.0.1-SNAPSHOT",
                description = """
                        API latihan Spring Boot: pengelolaan karyawan, payroll, dan customer.

                        Autentikasi memakai JWT bearer. Pakai tombol Authorize, pilih skema \
                        sesuai tipe akun, lalu isi username dan password — token diambil dan \
                        dipasang otomatis ke setiap permintaan, tidak perlu ditempel manual. \
                        Token berlaku 15 menit."""
        )
)
@SecurityScheme(
        name = "karyawanAuth",
        type = SecuritySchemeType.OAUTH2,
        description = "Akun karyawan: admin, manager, HR, marketing, sales, superadmin.",
        flows = @OAuthFlows(
                password = @OAuthFlow(tokenUrl = "/api/auth/karyawan/token")
        )
)
@SecurityScheme(
        name = "customerAuth",
        type = SecuritySchemeType.OAUTH2,
        description = "Akun customer yang mendaftar lewat /api/auth/customer/register.",
        flows = @OAuthFlows(
                password = @OAuthFlow(tokenUrl = "/api/auth/customer/token")
        )
)
public class OpenApiConfig {
}
