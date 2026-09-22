package com.arirahmat.helpdesk.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfigurasi Swagger UI / OpenAPI 3.
 * Setelah aplikasi jalan, dokumentasi interaktif tersedia di /swagger-ui.html
 * dan spesifikasi JSON mentah di /v3/api-docs.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI helpdeskOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Helpdesk Ticketing API")
                        .version("1.0.0")
                        .description("""
                                REST API manajemen tiket helpdesk.

                                Fitur: registrasi & login JWT, CRUD tiket dengan kontrol kepemilikan,
                                serta endpoint report agregasi untuk dashboard.

                                **Cara pakai di Swagger UI:**
                                1. Jalankan `POST /api/auth/register` lalu `POST /api/auth/login`.
                                2. Salin nilai `accessToken` dari response.
                                3. Klik tombol **Authorize** di kanan atas, tempel token tersebut.
                                4. Endpoint yang terkunci sekarang bisa dicoba lewat "Try it out".
                                """)
                        .contact(new Contact()
                                .name("Ari Rahmat Romadhon")
                                .url("https://github.com/arighmt67-bit"))
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .name(SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Tempel token dari /api/auth/login (tanpa prefix 'Bearer ')")));
    }
}
