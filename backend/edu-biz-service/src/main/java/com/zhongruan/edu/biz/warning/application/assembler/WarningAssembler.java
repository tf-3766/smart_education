package com.zhongruan.edu.biz.warning.application.assembler;

import com.zhongruan.edu.biz.warning.api.vo.CodeLabelVO;
import com.zhongruan.edu.biz.warning.api.vo.LearningWarningVO;
import com.zhongruan.edu.biz.warning.api.vo.WarningEvidenceVO;
import com.zhongruan.edu.biz.warning.domain.enums.WarningLevel;
import com.zhongruan.edu.biz.warning.domain.enums.WarningStatus;
import com.zhongruan.edu.biz.warning.domain.enums.WarningType;
import com.zhongruan.edu.biz.warning.infrastructure.persistence.entity.LearningWarningEntity;
import com.zhongruan.edu.biz.warning.infrastructure.persistence.entity.WarningEvidenceEntity;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WarningAssembler {
    public LearningWarningVO toVO(
            LearningWarningEntity warning,
            List<WarningEvidenceEntity> evidences,
            String studentName) {
        return new LearningWarningVO(
                id(warning.getId()),
                id(warning.getCourseId()),
                id(warning.getStudentId()),
                studentName,
                CodeLabelVO.of(WarningType.valueOf(warning.getWarningType())),
                CodeLabelVO.of(WarningLevel.valueOf(warning.getWarningLevel())),
                CodeLabelVO.of(WarningStatus.valueOf(warning.getWarningStatus())),
                warning.getSummary(),
                warning.getSuggestion(),
                id(warning.getAiExplanationDraftId()),
                time(warning.getGeneratedAt()),
                id(warning.getHandledBy()),
                time(warning.getHandledAt()),
                evidences.stream().map(this::toEvidence).toList(),
                warning.getVersion());
    }

    private WarningEvidenceVO toEvidence(WarningEvidenceEntity evidence) {
        return new WarningEvidenceVO(
                id(evidence.getId()),
                evidence.getEvidenceType(),
                id(evidence.getSourceId()),
                evidence.getMetricCode(),
                evidence.getMetricValue(),
                evidence.getDescription());
    }

    private OffsetDateTime time(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private String id(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
