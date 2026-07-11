package com.zhongruan.edu.biz.exam.api;

import static org.hamcrest.Matchers.hasItem;
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
class ExamApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teacherCanPublishAnExamPaperAndEnrolledStudentCanSeeTheExam() throws Exception {
        String teacherToken = login("teacher", "t123456");
        String studentToken = login("student", "123456");

        String bankId = body(mockMvc.perform(post("/api/v1/teacher/courses/21001/question-banks")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"考试接口测试题库\",\"description\":\"用于集成测试\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn()).path("data").path("bankId").asText();

        String questionId = body(mockMvc.perform(post("/api/v1/teacher/question-banks/{bankId}/questions", bankId)
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionType":"SINGLE_CHOICE",
                                  "stem":"哪一项是正确的接口测试断言？",
                                  "difficulty":"EASY",
                                  "score":5,
                                  "status":"ACTIVE",
                                  "options":[
                                    {"label":"A","content":"错误选项","correct":false,"sortOrder":10},
                                    {"label":"B","content":"正确选项","correct":true,"sortOrder":20}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.options[?(@.correct == true)].label").value(hasItem("B")))
                .andReturn()).path("data").path("questionId").asText();

        String examId = body(mockMvc.perform(post("/api/v1/teacher/courses/21001/exams")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"考试接口集成测试",
                                  "description":"发布后学生可见",
                                  "startAt":"2099-09-01T08:00:00+08:00",
                                  "endAt":"2099-09-01T10:00:00+08:00",
                                  "durationMinutes":60,
                                  "totalScore":10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()).path("data").path("examId").asText();

        String paperId = body(mockMvc.perform(post("/api/v1/teacher/exams/{examId}/papers", examId)
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"测试卷",
                                  "questions":[{"questionId":"%s","questionOrder":1,"score":10}]
                                }
                                """.formatted(questionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalScore").value(10))
                .andReturn()).path("data").path("paperId").asText();

        mockMvc.perform(post("/api/v1/teacher/exam-papers/{paperId}/publish", paperId)
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/student/courses/21001/exams")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].examId", hasItem(examId)))
                .andExpect(jsonPath("$.data.records[*].title", hasItem("考试接口集成测试")));

        mockMvc.perform(put("/api/v1/teacher/questions/{questionId}", questionId)
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionType":"SINGLE_CHOICE",
                                  "stem":"不允许修改已发布试卷中的题目",
                                  "difficulty":"EASY",
                                  "score":5,
                                  "options":[
                                    {"label":"A","content":"错误选项","correct":false,"sortOrder":10},
                                    {"label":"B","content":"正确选项","correct":true,"sortOrder":20}
                                  ],
                                  "version":0
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));
    }

    @Test
    void teacherCannotOperateAnotherTeachersCourseAndStudentCannotManageQuestionBanks() throws Exception {
        String teacherToken = login("teacher", "t123456");
        String otherTeacherToken = login("teacher2", "t123456");
        String studentToken = login("student", "123456");

        mockMvc.perform(post("/api/v1/teacher/courses/21003/question-banks")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"越权题库\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/teacher/courses/21001/question-banks")
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"学生越权题库\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/teacher/courses/21001/question-banks")
                        .header("Authorization", bearer(otherTeacherToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCanUpdateQuestionWithoutChangingItsStatus() throws Exception {
        String teacherToken = login("teacher", "t123456");

        String bankId = body(mockMvc.perform(post("/api/v1/teacher/courses/21001/question-banks")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"保留题目状态测试题库\"}"))
                .andExpect(status().isCreated())
                .andReturn()).path("data").path("bankId").asText();

        String questionId = body(mockMvc.perform(post("/api/v1/teacher/question-banks/{bankId}/questions", bankId)
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionType":"SINGLE_CHOICE",
                                  "stem":"更新前题目",
                                  "difficulty":"EASY",
                                  "score":5,
                                  "options":[
                                    {"label":"A","content":"错误选项","correct":false,"sortOrder":10},
                                    {"label":"B","content":"正确选项","correct":true,"sortOrder":20}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()).path("data").path("questionId").asText();

        mockMvc.perform(put("/api/v1/teacher/questions/{questionId}", questionId)
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionType":"SINGLE_CHOICE",
                                  "stem":"更新后题目",
                                  "difficulty":"MEDIUM",
                                  "score":6,
                                  "options":[
                                    {"label":"A","content":"错误选项","correct":false,"sortOrder":10},
                                    {"label":"B","content":"正确选项","correct":true,"sortOrder":20}
                                  ],
                                  "version":0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stem").value("更新后题目"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void teacherCannotDeleteQuestionReferencedByDraftPaper() throws Exception {
        String teacherToken = login("teacher", "t123456");

        String bankId = body(mockMvc.perform(post("/api/v1/teacher/courses/21001/question-banks")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"草稿试卷引用测试题库\"}"))
                .andExpect(status().isCreated())
                .andReturn()).path("data").path("bankId").asText();

        String questionId = body(mockMvc.perform(post("/api/v1/teacher/question-banks/{bankId}/questions", bankId)
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionType":"SINGLE_CHOICE",
                                  "stem":"草稿试卷中的题目",
                                  "difficulty":"EASY",
                                  "score":5,
                                  "options":[
                                    {"label":"A","content":"错误选项","correct":false,"sortOrder":10},
                                    {"label":"B","content":"正确选项","correct":true,"sortOrder":20}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()).path("data").path("questionId").asText();

        String examId = body(mockMvc.perform(post("/api/v1/teacher/courses/21001/exams")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"草稿试卷引用测试",
                                  "startAt":"2099-10-01T08:00:00+08:00",
                                  "endAt":"2099-10-01T10:00:00+08:00",
                                  "durationMinutes":60,
                                  "totalScore":10
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()).path("data").path("examId").asText();

        mockMvc.perform(post("/api/v1/teacher/exams/{examId}/papers", examId)
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"草稿试卷",
                                  "questions":[{"questionId":"%s","questionOrder":1,"score":10}]
                                }
                                """.formatted(questionId)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/teacher/questions/{questionId}", questionId)
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));
    }

    @Test
    void enrolledStudentCanStartResumeAndSubmitAnObjectiveExam() throws Exception {
        String teacherToken = login("teacher", "t123456");
        String studentToken = login("student", "123456");

        String examId = body(mockMvc.perform(post("/api/v1/teacher/courses/21001/exams")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"学生答题闭环测试",
                                  "startAt":"2020-01-01T00:00:00Z",
                                  "endAt":"2099-12-31T23:59:59Z",
                                  "durationMinutes":60,
                                  "totalScore":10
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()).path("data").path("examId").asText();
        String paperId = body(mockMvc.perform(post("/api/v1/teacher/exams/{examId}/papers", examId)
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"学生答题闭环试卷",
                                  "questions":[{"questionId":"37101","questionOrder":1,"score":10}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()).path("data").path("paperId").asText();
        mockMvc.perform(post("/api/v1/teacher/exam-papers/{paperId}/publish", paperId)
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk());

        MvcResult startResult = mockMvc.perform(post("/api/v1/student/exams/{examId}/attempts", examId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.questions[0].questionId").value("37101"))
                .andExpect(jsonPath("$.data.questions[0].options[1].label").value("B"))
                .andExpect(jsonPath("$.data.questions[0].options[1].correct").doesNotExist())
                .andReturn();
        String attemptId = body(startResult).path("data").path("attemptId").asText();

        mockMvc.perform(get("/api/v1/student/exam-attempts/{attemptId}", attemptId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attemptId").value(attemptId))
                .andExpect(jsonPath("$.data.version").value(0));

        mockMvc.perform(post("/api/v1/student/exam-attempts/{attemptId}/submit", attemptId)
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers":[{"questionId":"37101","answerContent":"B"}],
                                  "version":0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("GRADED"))
                .andExpect(jsonPath("$.data.score").value(10))
                .andExpect(jsonPath("$.data.answers[0].score").value(10));

        mockMvc.perform(post("/api/v1/student/exam-attempts/{attemptId}/submit", attemptId)
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"questionId":"37101","answerContent":"B"}],"version":1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPERATION_NOT_ALLOWED"));
    }

    @Test
    void teacherCanGradeSubmittedShortAnswerExam() throws Exception {
        String teacherToken = login("teacher", "t123456");
        String studentToken = login("student", "123456");
        String bankId = body(mockMvc.perform(post("/api/v1/teacher/courses/21001/question-banks")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"简答题批阅测试题库\"}"))
                .andExpect(status().isCreated())
                .andReturn()).path("data").path("bankId").asText();
        String questionId = body(mockMvc.perform(post("/api/v1/teacher/question-banks/{bankId}/questions", bankId)
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionType":"SHORT_ANSWER",
                                  "stem":"请说明课程边界。",
                                  "difficulty":"MEDIUM",
                                  "score":10,
                                  "options":[]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()).path("data").path("questionId").asText();
        String examId = body(mockMvc.perform(post("/api/v1/teacher/courses/21001/exams")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"简答题批阅测试",
                                  "startAt":"2020-01-01T00:00:00Z",
                                  "endAt":"2099-12-31T23:59:59Z",
                                  "durationMinutes":60,
                                  "totalScore":10
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()).path("data").path("examId").asText();
        String paperId = body(mockMvc.perform(post("/api/v1/teacher/exams/{examId}/papers", examId)
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"简答题试卷","questions":[{"questionId":"%s","questionOrder":1,"score":10}]}
                                """.formatted(questionId)))
                .andExpect(status().isCreated())
                .andReturn()).path("data").path("paperId").asText();
        mockMvc.perform(post("/api/v1/teacher/exam-papers/{paperId}/publish", paperId)
                        .header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk());
        String attemptId = body(mockMvc.perform(post("/api/v1/student/exams/{examId}/attempts", examId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andReturn()).path("data").path("attemptId").asText();
        mockMvc.perform(post("/api/v1/student/exam-attempts/{attemptId}/submit", attemptId)
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"questionId":"%s","answerContent":"正式业务事实由 Biz 维护。"}],"version":0}
                                """.formatted(questionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        mockMvc.perform(get("/api/v1/teacher/exams/{examId}/attempts", examId)
                        .header("Authorization", bearer(teacherToken))
                        .param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[*].attemptId", hasItem(attemptId)));
        mockMvc.perform(post("/api/v1/teacher/exam-attempts/{attemptId}/grade", attemptId)
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"questionId":"%s","score":8,"teacherComment":"边界说明正确。"}],"version":1}
                                """.formatted(questionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("GRADED"))
                .andExpect(jsonPath("$.data.score").value(8))
                .andExpect(jsonPath("$.data.gradedAt").exists())
                .andExpect(jsonPath("$.data.answers[0].score").value(8))
                .andExpect(jsonPath("$.data.answers[0].teacherComment").value("边界说明正确。"));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Credentials(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Credentials(String username, String password) {}
}
