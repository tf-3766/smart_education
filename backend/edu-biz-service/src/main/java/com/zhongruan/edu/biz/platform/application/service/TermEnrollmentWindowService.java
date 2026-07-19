package com.zhongruan.edu.biz.platform.application.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhongruan.edu.biz.platform.api.dto.request.UpsertTermEnrollmentWindowRequest;
import com.zhongruan.edu.biz.platform.api.vo.TermEnrollmentWindowVO;
import com.zhongruan.edu.biz.platform.infrastructure.persistence.entity.TermEnrollmentWindowEntity;
import com.zhongruan.edu.biz.platform.infrastructure.persistence.mapper.TermEnrollmentWindowMapper;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TermEnrollmentWindowService {
    private final TermEnrollmentWindowMapper mapper;

    public TermEnrollmentWindowService(TermEnrollmentWindowMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TermEnrollmentWindowVO> list() {
        return mapper.selectList(Wrappers.<TermEnrollmentWindowEntity>lambdaQuery()
                        .orderByAsc(TermEnrollmentWindowEntity::getTerm))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public TermEnrollmentWindowVO upsert(UpsertTermEnrollmentWindowRequest request) {
        validateTimes(request.enrollmentOpenAt(), request.enrollmentCloseAt());
        String term = request.term().trim();
        TermEnrollmentWindowEntity entity = mapper.selectOne(Wrappers.<TermEnrollmentWindowEntity>lambdaQuery()
                .eq(TermEnrollmentWindowEntity::getTerm, term));
        if (entity == null) {
            entity = new TermEnrollmentWindowEntity();
            entity.setTerm(term);
            entity.setEnrollmentOpenAt(utc(request.enrollmentOpenAt()));
            entity.setEnrollmentCloseAt(utc(request.enrollmentCloseAt()));
            try {
                mapper.insert(entity);
            } catch (DuplicateKeyException exception) {
                throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "该学期窗口已被其他请求创建，请刷新后重试");
            }
        } else {
            entity.setEnrollmentOpenAt(utc(request.enrollmentOpenAt()));
            entity.setEnrollmentCloseAt(utc(request.enrollmentCloseAt()));
            if (mapper.updateById(entity) != 1) {
                throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "学期窗口已被其他请求修改，请刷新后重试");
            }
        }
        return toVO(entity);
    }

    private void validateTimes(OffsetDateTime openAt, OffsetDateTime closeAt) {
        if (openAt != null && closeAt != null && !closeAt.isAfter(openAt)) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "选课截止时间必须晚于开始时间");
        }
    }

    private LocalDateTime utc(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private TermEnrollmentWindowVO toVO(TermEnrollmentWindowEntity entity) {
        return new TermEnrollmentWindowVO(
                String.valueOf(entity.getId()),
                entity.getTerm(),
                time(entity.getEnrollmentOpenAt()),
                time(entity.getEnrollmentCloseAt()),
                entity.getVersion());
    }

    private OffsetDateTime time(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
