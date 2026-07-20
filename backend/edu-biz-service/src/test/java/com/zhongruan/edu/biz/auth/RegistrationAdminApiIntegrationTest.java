package com.zhongruan.edu.biz.auth;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationAdminApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void studentsRegisterImmediatelyAndTeachersRequireSuperAdministratorApproval() throws Exception {
        MvcResult studentRegistration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"New.Student",
                                  "password":"Student2026",
                                  "displayName":"新学生",
                                  "role":"STUDENT"
                                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userStatus").value("ENABLED"))
                .andExpect(jsonPath("$.data.approvalRequired").value(false))
                .andExpect(jsonPath("$.data.login.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.login.user.username").value("new.student"))
                .andExpect(jsonPath("$.data.login.user.activeRole").value("STUDENT"))
                .andExpect(jsonPath("$.data.login.roles", hasItem("STUDENT")))
                .andExpect(jsonPath("$.data.login.permissions", hasItem("student:access")))
                .andReturn();

        String studentToken = body(studentRegistration).path("data").path("login").path("accessToken").asText();
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("新学生"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"NEW.STUDENT\",\"password\":\"Student2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("new.student"));

        MvcResult teacherRegistration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"new.teacher",
                                  "password":"Teacher2026",
                                  "displayName":"新教师",
                                  "role":"TEACHER"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.role").value("TEACHER"))
                .andExpect(jsonPath("$.data.userStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.approvalRequired").value(true))
                .andExpect(jsonPath("$.data.login").value(nullValue()))
                .andReturn();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new.teacher\",\"password\":\"Teacher2026\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_PENDING_APPROVAL"));

        String superAdminToken = token(login("admin", "admin123"));
        String teacherId = body(teacherRegistration).path("data").path("userId").asText();
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", bearer(superAdminToken))
                        .param("status", "PENDING")
                        .param("keyword", "new.teacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].userId").value(teacherId));
        mockMvc.perform(put("/api/v1/admin/users/{userId}/teacher-approval", teacherId)
                        .header("Authorization", bearer(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userStatus").value("ENABLED"))
                .andExpect(jsonPath("$.data.roles", hasItem("TEACHER")));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new.teacher\",\"password\":\"Teacher2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.activeRole").value("TEACHER"));
    }

    @Test
    void registrationRejectsDuplicateUsernameWeakPasswordAndPrivilegedRole() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"STUDENT",
                                  "password":"Student2026",
                                  "displayName":"重复学生",
                                  "role":"STUDENT"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"weak.password",
                                  "password":"password",
                                  "displayName":"弱密码用户",
                                  "role":"STUDENT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'password')].rejectedValue")
                        .value(org.hamcrest.Matchers.everyItem(nullValue())));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"self.admin",
                                  "password":"Admin2026",
                                  "displayName":"越权管理员",
                                  "role":"ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_VALIDATION_ERROR"));
    }

    @Test
    void rejectedTeacherCannotLoginOrBeReviewedAgainAndOrdinaryTeacherCannotReview() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"rejected.teacher",
                                  "password":"Teacher2026",
                                  "displayName":"待拒绝教师",
                                  "role":"TEACHER"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andReturn();
        String teacherId = body(registration).path("data").path("userId").asText();
        String ordinaryTeacherToken = token(login("teacher", "t123456"));
        String superAdminToken = token(login("admin", "admin123"));

        mockMvc.perform(put("/api/v1/admin/users/{userId}/teacher-approval", teacherId)
                        .header("Authorization", bearer(ordinaryTeacherToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(delete("/api/v1/admin/users/{userId}/teacher-approval", teacherId)
                        .header("Authorization", bearer(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userStatus").value("REJECTED"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rejected.teacher\",\"password\":\"Teacher2026\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));

        mockMvc.perform(put("/api/v1/admin/users/{userId}/teacher-approval", teacherId)
                        .header("Authorization", bearer(superAdminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEACHER_REGISTRATION_NOT_PENDING"));
    }

    @Test
    void onlySuperAdministratorCanGrantAndRevokeAdministratorRole() throws Exception {
        MvcResult superAdminLogin = login("admin", "admin123");
        String superAdminToken = token(superAdminLogin);
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeRole").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.roles", hasItem("ADMIN")))
                .andExpect(jsonPath("$.data.roles", hasItem("SUPER_ADMIN")))
                .andExpect(jsonPath("$.data.permissions", hasItem("admin:manage")));
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", bearer(superAdminToken))
                        .param("keyword", "teacher2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].userId").value("1004"))
                .andExpect(jsonPath("$.data.records[0].roles", hasItem("TEACHER")));

        mockMvc.perform(put("/api/v1/admin/users/1004/administrator")
                        .header("Authorization", bearer(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles", hasItem("TEACHER")))
                .andExpect(jsonPath("$.data.roles", hasItem("ADMIN")))
                .andExpect(jsonPath("$.data.superAdministrator").value(false));

        MvcResult ordinaryAdminLogin = login("teacher2", "t123456");
        String ordinaryAdminToken = token(ordinaryAdminLogin);
        mockMvc.perform(put("/api/v1/admin/users/1001/administrator")
                        .header("Authorization", bearer(ordinaryAdminToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(delete("/api/v1/admin/users/1004/administrator")
                        .header("Authorization", bearer(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles", not(hasItem("ADMIN"))))
                .andExpect(jsonPath("$.data.roles", hasItem("TEACHER")));

        mockMvc.perform(put("/api/v1/admin/users/1004/administrator")
                        .header("Authorization", bearer(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles", hasItem("ADMIN")));

        mockMvc.perform(delete("/api/v1/admin/users/1003/administrator")
                        .header("Authorization", bearer(superAdminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUPER_ADMIN_PROTECTED"));
    }

    private MvcResult login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Credentials(username, password))))
                .andExpect(status().isOk())
                .andReturn();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String token(MvcResult result) throws Exception {
        return body(result).path("data").path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Credentials(String username, String password) {}
}
