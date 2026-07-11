package com.zhongruan.edu.biz.storage.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FileStorageApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userUploadsBindsAndReadsAvatar() throws Exception {
        Session student = login("student", "123456");
        Session teacher = login("teacher", "t123456");
        byte[] image = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01, 0x02, 0x03, 0x04
        };

        JsonNode uploaded = upload(student.token(), "avatar.png", "image/png", image, "AVATAR");
        String fileId = uploaded.path("fileId").asText();

        mockMvc.perform(put("/api/v1/auth/me/avatar")
                        .header("Authorization", bearer(student.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":\"%s\",\"version\":%d}".formatted(fileId, student.version())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarFileId").value(fileId))
                .andExpect(jsonPath("$.data.avatarUrl").value("/api/v1/files/" + fileId + "/content"));

        mockMvc.perform(get("/api/v1/files/{fileId}/content", fileId)
                        .header("Authorization", bearer(teacher.token())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(image));

        mockMvc.perform(delete("/api/v1/files/{fileId}", fileId)
                        .header("Authorization", bearer(student.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FILE_IN_USE"));
    }

    @Test
    void managedCourseMaterialCanBeDownloadedByEnrolledStudent() throws Exception {
        Session teacher = login("teacher", "t123456");
        Session student = login("student", "123456");
        byte[] document = "course-material".getBytes(StandardCharsets.UTF_8);
        String fileId = upload(teacher.token(), "lesson.pdf", "application/pdf", document, "COURSE_MATERIAL")
                .path("fileId")
                .asText();

        MvcResult createResult = mockMvc.perform(post("/api/v1/teacher/courses/21001/materials")
                        .header("Authorization", bearer(teacher.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Managed lesson file",
                                  "materialType":"DOCUMENT",
                                  "fileId":"%s",
                                  "visibility":"COURSE",
                                  "status":"PUBLISHED",
                                  "sortOrder":99
                                }
                                """.formatted(fileId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fileId").value(fileId))
                .andReturn();
        String materialId = body(createResult).path("data").path("materialId").asText();

        mockMvc.perform(get("/api/v1/student/materials/{materialId}", materialId)
                        .header("Authorization", bearer(student.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessMode").value("MANAGED_FILE"))
                .andExpect(jsonPath("$.data.accessUrl").value("/api/v1/files/" + fileId + "/content"));

        mockMvc.perform(get("/api/v1/files/{fileId}/content", fileId)
                        .header("Authorization", bearer(student.token())))
                .andExpect(status().isOk())
                .andExpect(content().bytes(document));
    }

    @Test
    void avatarRejectsUnsafeTypeAndAnonymousUpload() throws Exception {
        Session student = login("student", "123456");
        MockMultipartFile text = new MockMultipartFile(
                "file", "avatar.txt", "text/plain", "not-an-image".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/files")
                        .file(text)
                        .param("purpose", "AVATAR")
                        .header("Authorization", bearer(student.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_TYPE_NOT_ALLOWED"));

        MockMultipartFile spoofedImage = new MockMultipartFile(
                "file", "avatar.png", "image/png", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/files")
                        .file(spoofedImage)
                        .param("purpose", "AVATAR")
                        .header("Authorization", bearer(student.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_TYPE_NOT_ALLOWED"));

        mockMvc.perform(multipart("/api/v1/files").file(text).param("purpose", "GENERAL"))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode upload(String token, String name, String mimeType, byte[] bytes, String purpose) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", name, mimeType, bytes);
        MvcResult result = mockMvc.perform(multipart("/api/v1/files")
                        .file(file)
                        .param("purpose", purpose)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sha256").isNotEmpty())
                .andReturn();
        return body(result).path("data");
    }

    private Session login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Credentials(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = body(result).path("data");
        return new Session(data.path("accessToken").asText(), data.path("user").path("version").asInt());
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Credentials(String username, String password) {}

    private record Session(String token, int version) {}
}
