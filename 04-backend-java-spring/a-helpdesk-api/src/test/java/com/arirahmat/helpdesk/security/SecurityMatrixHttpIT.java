package com.arirahmat.helpdesk.security;

import com.arirahmat.helpdesk.entity.Role;
import com.arirahmat.helpdesk.entity.User;
import com.arirahmat.helpdesk.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pengujian lewat container Tomcat sungguhan pada port acak.
 *
 * <p>MockMvc tidak menjalankan ERROR dispatch milik servlet container, sehingga regresi
 * "403 berubah menjadi 401 karena diteruskan ke /error" tidak akan tertangkap di sana.
 * Kelas ini memakai HTTP nyata supaya perilaku dispatch container ikut teruji:
 * body 403 wajib menyebut URI aslinya, bukan {@code /error}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Matriks keamanan lewat HTTP nyata (Tomcat)")
class SecurityMatrixHttpIT {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userToken;

    @BeforeEach
    void setUp() {
        String email = "http-user-" + UUID.randomUUID() + "@example.com";
        String body = """
                {"email":"%s","password":"rahasia123","fullName":"Pengguna HTTP"}
                """.formatted(email);

        ResponseEntity<String> res = rest.exchange("/api/auth/register", HttpMethod.POST,
                jsonEntity(body, null), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        userToken = readJson(res.getBody()).get("accessToken").asText();
    }

    @Test
    @DisplayName("aplikasi benar-benar boot: jjwt-impl tersedia saat runtime")
    void aplikasiBootDanMenerbitkanToken() {
        assertThat(userToken).isNotBlank();
        assertThat(userToken.split("\\.")).hasSize(3);
        assertThat(jwtService.parse(userToken)).isPresent();
    }

    @Test
    @DisplayName("tanpa token membalas 401 dengan path asli")
    void tanpaTokenBalas401() {
        ResponseEntity<String> res = rest.exchange("/api/tickets", HttpMethod.GET,
                jsonEntity(null, null), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(readJson(res.getBody()).get("path").asText()).isEqualTo("/api/tickets");
    }

    @Test
    @DisplayName("role kurang membalas 403 dan path bukan /error")
    void roleKurangBalas403BukanErrorPath() {
        ResponseEntity<String> res = rest.exchange("/api/reports/summary", HttpMethod.GET,
                jsonEntity(null, userToken), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        JsonNode body = readJson(res.getBody());
        assertThat(body.get("status").asInt()).isEqualTo(403);
        assertThat(body.get("path").asText())
                .as("403 yang di-forward ke /error akan disalahartikan sebagai 401")
                .isNotEqualTo("/error")
                .isEqualTo("/api/reports/summary");
    }

    @Test
    @DisplayName("role cukup membalas 200")
    void roleCukupBalas200() {
        String email = "http-agent-" + UUID.randomUUID() + "@example.com";
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("rahasia123"))
                .fullName("Petugas HTTP")
                .role(Role.ROLE_AGENT)
                .build());
        String agentToken = jwtService.generateToken(email, Role.ROLE_AGENT.name());

        ResponseEntity<String> res = rest.exchange("/api/reports/summary", HttpMethod.GET,
                jsonEntity(null, agentToken), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readJson(res.getBody()).has("totalTickets")).isTrue();
    }

    @Test
    @DisplayName("token valid membalas 200 pada endpoint tiket")
    void tokenValidBalas200() {
        ResponseEntity<String> res = rest.exchange("/api/tickets", HttpMethod.GET,
                jsonEntity(null, userToken), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ---------------------------------------------------------------- helper

    private HttpEntity<String> jsonEntity(String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(body, headers);
    }

    private JsonNode readJson(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Response bukan JSON valid: " + raw, ex);
        }
    }
}
