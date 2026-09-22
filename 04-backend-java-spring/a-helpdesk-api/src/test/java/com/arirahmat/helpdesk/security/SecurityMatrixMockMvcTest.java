package com.arirahmat.helpdesk.security;

import com.arirahmat.helpdesk.entity.Role;
import com.arirahmat.helpdesk.entity.User;
import com.arirahmat.helpdesk.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriks status keamanan pada lapisan HTTP.
 *
 * <p>Test unit murni pernah hijau 29/29 sementara aplikasinya tidak bisa boot dan membalas
 * 403 untuk request anonim. Kelas ini menutup celah tersebut: setiap baris di bawah adalah
 * kontrak yang harus dipenuhi seluruh filter chain, bukan sekadar satu kelas terisolasi.
 *
 * <pre>
 * | Request                                   | Harapan |
 * |-------------------------------------------|---------|
 * | tanpa header Authorization                | 401     |
 * | token sampah / bukan JWT                  | 401     |
 * | skema non-Bearer (Basic ...)              | 401     |
 * | token kedaluwarsa                         | 401     |
 * | tanda tangan salah                        | 401     |
 * | issuer salah                              | 401     |
 * | token valid, role cukup                   | 200     |
 * | token valid, role kurang (USER-&gt;reports)  | 403     |
 * | token valid, tiket milik orang lain       | 403     |
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Matriks keamanan HTTP 401/403/200")
class SecurityMatrixMockMvcTest {

    private static final String TICKETS = "/api/tickets";
    private static final String REPORTS = "/api/reports/summary";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userToken;
    private String agentToken;
    private String userEmail;

    @BeforeEach
    void setUp() throws Exception {
        userEmail = "user-" + UUID.randomUUID() + "@example.com";
        userToken = registerAndGetToken(userEmail);
        agentToken = tokenForSeededUser(Role.ROLE_AGENT);
    }

    // ---------------------------------------------------------------- 401

    @Nested
    @DisplayName("401 Unauthorized: identitas belum terbukti")
    class Unauthorized {

        @Test
        @DisplayName("tanpa header Authorization membalas 401, bukan 403")
        void tanpaHeader() throws Exception {
            mockMvc.perform(get(TICKETS))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Token tidak ada atau tidak valid"))
                    .andExpect(jsonPath("$.path").value(TICKETS));
        }

        @Test
        @DisplayName("token sampah membalas 401")
        void tokenSampah() throws Exception {
            mockMvc.perform(get(TICKETS).header("Authorization", "Bearer bukan.token.jwt"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("skema non-Bearer membalas 401")
        void skemaBasic() throws Exception {
            String basic = Base64.getEncoder()
                    .encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));
            mockMvc.perform(get(TICKETS).header("Authorization", "Basic " + basic))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("header Bearer kosong membalas 401")
        void bearerKosong() throws Exception {
            mockMvc.perform(get(TICKETS).header("Authorization", "Bearer "))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("token kedaluwarsa membalas 401")
        void tokenKedaluwarsa() throws Exception {
            mockMvc.perform(get(TICKETS).header("Authorization", "Bearer " + expiredToken()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("tanda tangan salah membalas 401")
        void tandaTanganSalah() throws Exception {
            mockMvc.perform(get(TICKETS).header("Authorization", "Bearer " + wrongSignatureToken()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("issuer salah membalas 401")
        void issuerSalah() throws Exception {
            mockMvc.perform(get(TICKETS).header("Authorization", "Bearer " + wrongIssuerToken()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("login dengan password salah membalas 401")
        void passwordSalah() throws Exception {
            String body = """
                    {"email":"%s","password":"password-yang-salah"}
                    """.formatted(userEmail);
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Email atau password salah"));
        }
    }

    // ---------------------------------------------------------------- 403

    @Nested
    @DisplayName("403 Forbidden: identitas terbukti, wewenang kurang")
    class Forbidden {

        @Test
        @DisplayName("ROLE_USER menuju endpoint report membalas 403, bukan 401")
        void userKeReport() throws Exception {
            mockMvc.perform(get(REPORTS).header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_USER tidak boleh membaca tiket milik orang lain")
        void tiketOrangLain() throws Exception {
            String lain = "lain-" + UUID.randomUUID() + "@example.com";
            String tokenLain = registerAndGetToken(lain);
            long idMilikOrangLain = createTicket(tokenLain);

            mockMvc.perform(get(TICKETS + "/" + idMilikOrangLain)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.path").value(TICKETS + "/" + idMilikOrangLain));
        }

        @Test
        @DisplayName("ROLE_USER tidak boleh mengubah status tiket")
        void userUbahStatus() throws Exception {
            long id = createTicket(userToken);
            mockMvc.perform(patch(TICKETS + "/" + id + "/status")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"RESOLVED\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------- 200

    @Nested
    @DisplayName("200/201: token valid dan wewenang cukup")
    class Authorized {

        @Test
        @DisplayName("token valid membaca daftar tiket membalas 200")
        void daftarTiket() throws Exception {
            mockMvc.perform(get(TICKETS).header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("token valid membuat tiket membalas 201")
        void buatTiket() throws Exception {
            mockMvc.perform(post(TICKETS)
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title":"Printer lantai 3 macet",
                                     "description":"Paper jam saat duplex",
                                     "priority":"HIGH"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    .andExpect(jsonPath("$.ownerEmail").value(userEmail));
        }

        @Test
        @DisplayName("ROLE_AGENT boleh membaca endpoint report")
        void agentKeReport() throws Exception {
            mockMvc.perform(get(REPORTS).header("Authorization", "Bearer " + agentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTickets").exists());
        }

        @Test
        @DisplayName("endpoint publik tetap terbuka tanpa token")
        void endpointPublik() throws Exception {
            mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("payload tidak valid membalas 400, bukan 401/403")
        void payloadTidakValid() throws Exception {
            mockMvc.perform(post(TICKETS)
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"\",\"description\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
        }
    }

    // ---------------------------------------------------------------- klaim token

    @Test
    @DisplayName("token yang diterbitkan memiliki exp - iat tepat 1800 detik")
    void masaBerlakuToken() throws Exception {
        JsonNode payload = decodePayload(userToken);
        long iat = payload.get("iat").asLong();
        long exp = payload.get("exp").asLong();

        assertThat(exp - iat).isEqualTo(1800L);
        assertThat(payload.get("iss").asText()).isEqualTo("helpdesk-api");
        assertThat(payload.get("role").asText()).isEqualTo("ROLE_USER");
        assertThat(payload.get("sub").asText()).isEqualTo(userEmail);
    }

    // ---------------------------------------------------------------- helper

    private String registerAndGetToken(String email) throws Exception {
        String body = """
                {"email":"%s","password":"rahasia123","fullName":"Pengguna Uji"}
                """.formatted(email);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    /** Registrasi publik selalu ROLE_USER, jadi akun ber-role tinggi disemai langsung. */
    private String tokenForSeededUser(Role role) {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("rahasia123"))
                .fullName("Petugas Uji")
                .role(role)
                .build());
        return jwtService.generateToken(email, role.name());
    }

    private long createTicket(String token) throws Exception {
        MvcResult result = mockMvc.perform(post(TICKETS)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Tiket uji","description":"Dibuat oleh test","priority":"LOW"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private SecretKey realKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    private String expiredToken() {
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        return Jwts.builder()
                .subject(userEmail).claim("role", "ROLE_USER").issuer(jwtProperties.issuer())
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plus(30, ChronoUnit.MINUTES)))
                .signWith(realKey())
                .compact();
    }

    private String wrongSignatureToken() {
        SecretKey lain = Keys.hmacShaKeyFor(
                "kunci-penyerang-yang-panjangnya-juga-cukup-untuk-hs384".getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userEmail).claim("role", "ROLE_ADMIN").issuer(jwtProperties.issuer())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)))
                .signWith(lain)
                .compact();
    }

    private String wrongIssuerToken() {
        return Jwts.builder()
                .subject(userEmail).claim("role", "ROLE_ADMIN").issuer("penerbit-palsu")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)))
                .signWith(realKey())
                .compact();
    }

    private JsonNode decodePayload(String token) throws Exception {
        String segment = token.split("\\.")[1];
        byte[] decoded = Base64.getUrlDecoder().decode(segment);
        return objectMapper.readTree(decoded);
    }
}
