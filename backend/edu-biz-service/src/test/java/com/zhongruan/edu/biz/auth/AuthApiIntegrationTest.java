package com.zhongruan.edu.biz.auth;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest {
    private static final String TRACE_ID = "test-trace-12345678";
    private static final String JWT_SECRET = "test-only-jwt-secret-with-at-least-32-bytes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginSucceedsWithStudentAccountAndReturnsUnifiedEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Trace-Id", TRACE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"student","password":"Student@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", TRACE_ID))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").isNumber())
                .andExpect(jsonPath("$.data.user.userId").value("1001"))
                .andExpect(jsonPath("$.data.roles", hasItem("STUDENT")))
                .andExpect(jsonPath("$.data.permissions", hasItem("student:access")));
    }

    @Test
    void loginFailsWithoutLeakingCredentialDetails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"student","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void protectedEndpointRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void illegalTokenUsesUnifiedUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void expiredTokenUsesDedicatedErrorCode() throws Exception {
        String token = expiredToken();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void studentCannotAccessTeacherTestEndpoint() throws Exception {
        String accessToken = loginAndGetToken("student", "Student@123");

        mockMvc.perform(get("/api/v1/test/teacher")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void teacherCannotAccessAdminTestEndpoint() throws Exception {
        String accessToken = loginAndGetToken("teacher", "Teacher@123");

        mockMvc.perform(get("/api/v1/test/admin")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void eachRoleCanAccessItsOwnTestEndpoint() throws Exception {
        for (Credentials credentials : List.of(
                new Credentials("student", "Student@123"),
                new Credentials("teacher", "Teacher@123"),
                new Credentials("admin", "Admin@123"))) {
            String accessToken = loginAndGetToken(credentials.username(), credentials.password());

            mockMvc.perform(get("/api/v1/test/" + credentials.username())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.scope").value(credentials.username().toUpperCase()))
                    .andExpect(jsonPath("$.data.result").value("ALLOWED"))
                    .andExpect(jsonPath("$.traceId").isNotEmpty());
        }
    }

    @Test
    void invalidLoginRequestUsesValidationErrorEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.errors[?(@.field == 'password')].rejectedValue")
                        .value(everyItem(nullValue())))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void authenticatedUserCanReadCurrentIdentity() throws Exception {
        String accessToken = loginAndGetToken("teacher", "Teacher@123");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("teacher"))
                .andExpect(jsonPath("$.data.activeRole").value("TEACHER"))
                .andExpect(jsonPath("$.data.permissions", hasItem("teacher:access")))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void logoutAcknowledgesClientSideTokenDiscard() throws Exception {
        String accessToken = loginAndGetToken("student", "Student@123");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("CLIENT_DISCARD_TOKEN"))
                .andExpect(jsonPath("$.data.serverSideRevoked").value(false))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Credentials(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("accessToken").asText();
    }

    private String expiredToken() {
        return Jwts.builder()
                .issuer("edu-biz-service-test")
                .subject("1001")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .claim("username", "student")
                .claim("activeRole", "STUDENT")
                .claim("roles", List.of("STUDENT"))
                .claim("permissions", List.of("student:access"))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private record Credentials(String username, String password) {}
}
