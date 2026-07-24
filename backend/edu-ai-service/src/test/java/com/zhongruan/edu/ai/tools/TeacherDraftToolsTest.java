package com.zhongruan.edu.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhongruan.edu.ai.api.vo.AiDraftVO;
import com.zhongruan.edu.ai.application.AiApplicationService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class TeacherDraftToolsTest {
    @Test
    void warningCommandUsesExistingAuthorizedDraftServiceAndEmitsResultCard() {
        AiApplicationService service = mock(AiApplicationService.class);
        when(service.warningInterventionPlan(any(), any(), any(), eq(36001L), any(), any()))
                .thenReturn(Mono.just(new AiDraftVO(
                        "draft-1", "RISK_INTERVENTION_PLAN", "36001", "补救与复查计划", "test", "m",
                        "DRAFT", List.of(), OffsetDateTime.now())));
        var events = new ArrayList<com.zhongruan.edu.ai.api.vo.AiActionVO>();
        TeacherDraftTools tools = new TeacherDraftTools(
                service, "Bearer teacher", 1002L, "TEACHER", "trace", events::add);

        assertThat(tools.riskIntervention("36001", "三天后复查")).contains("补救与复查计划");
        verify(service).warningInterventionPlan(
                "Bearer teacher", 1002L, "TEACHER", 36001L, "三天后复查", "trace");
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.capabilityId()).isEqualTo("course.risk-intervention.plan");
            assertThat(event.status()).isEqualTo("DRAFT_CREATED");
        });
    }

    @Test
    void paperSuggestionCommandUsesAuthorizedQuestionBankService() {
        AiApplicationService service = mock(AiApplicationService.class);
        when(service.paperSuggestion(any(), any(), any(), eq(21001L), eq(20),
                eq(new BigDecimal("100")), any(), any()))
                .thenReturn(Mono.just(new AiDraftVO(
                        "paper-1", "PAPER_SUGGESTION", "21001", "单选10题、简答2题", "test", "m",
                        "DRAFT", List.of(), OffsetDateTime.now())));
        TeacherDraftTools tools = new TeacherDraftTools(
                service, "Bearer teacher", 1002L, "TEACHER", "trace", ignored -> {});

        assertThat(tools.paperSuggestion("21001", 20, new BigDecimal("100"), "覆盖前三章"))
                .contains("单选10题");
        verify(service).paperSuggestion(
                "Bearer teacher", 1002L, "TEACHER", 21001L, 20,
                new BigDecimal("100"), "覆盖前三章", "trace");
    }
}
