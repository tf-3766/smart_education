package com.zhongruan.edu.biz.platform.application.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMapper;
import com.zhongruan.edu.biz.platform.api.dto.request.CreateCourseCategoryRequest;
import com.zhongruan.edu.biz.platform.api.dto.request.UpdateCourseCategoryRequest;
import com.zhongruan.edu.biz.platform.api.vo.CourseCategoryVO;
import com.zhongruan.edu.biz.platform.infrastructure.persistence.entity.CourseCategoryEntity;
import com.zhongruan.edu.biz.platform.infrastructure.persistence.mapper.CourseCategoryMapper;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseCategoryService {
    private final CourseCategoryMapper categoryMapper;
    private final CourseMapper courseMapper;

    public CourseCategoryService(CourseCategoryMapper categoryMapper, CourseMapper courseMapper) {
        this.categoryMapper = categoryMapper;
        this.courseMapper = courseMapper;
    }

    @Transactional(readOnly = true)
    public List<CourseCategoryVO> listEnabled() {
        return list(true);
    }

    @Transactional(readOnly = true)
    public List<CourseCategoryVO> listForAdministration() {
        return list(false);
    }

    @Transactional
    public CourseCategoryVO create(CreateCourseCategoryRequest request) {
        CourseCategoryEntity category = new CourseCategoryEntity();
        category.setName(request.name().trim());
        category.setSortOrder(request.sortOrder());
        category.setEnabled(Boolean.TRUE.equals(request.enabled()) ? 1 : 0);
        try {
            categoryMapper.insert(category);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "课程分类名称已存在");
        }
        return toVO(category);
    }

    @Transactional
    public CourseCategoryVO update(Long categoryId, UpdateCourseCategoryRequest request) {
        CourseCategoryEntity category = requireCategory(categoryId);
        category.setName(request.name().trim());
        category.setSortOrder(request.sortOrder());
        category.setEnabled(Boolean.TRUE.equals(request.enabled()) ? 1 : 0);
        category.setVersion(request.version());
        try {
            if (categoryMapper.updateById(category) != 1) {
                throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT);
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "课程分类名称已存在");
        }
        return toVO(category);
    }

    @Transactional
    public void delete(Long categoryId) {
        requireCategory(categoryId);
        if (courseMapper.selectCount(Wrappers.<CourseEntity>lambdaQuery()
                        .eq(CourseEntity::getCategoryId, categoryId)) > 0) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "课程分类正在被课程使用，不能删除");
        }
        categoryMapper.deleteById(categoryId);
    }

    @Transactional(readOnly = true)
    public void requireEnabled(Long categoryId) {
        CourseCategoryEntity category = requireCategory(categoryId);
        if (category.getEnabled() == null || category.getEnabled() != 1) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "课程分类已停用");
        }
    }

    private List<CourseCategoryVO> list(boolean enabledOnly) {
        var wrapper = Wrappers.<CourseCategoryEntity>lambdaQuery();
        if (enabledOnly) {
            wrapper.eq(CourseCategoryEntity::getEnabled, 1);
        }
        return categoryMapper.selectList(wrapper
                        .orderByAsc(CourseCategoryEntity::getSortOrder)
                        .orderByAsc(CourseCategoryEntity::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private CourseCategoryEntity requireCategory(Long categoryId) {
        CourseCategoryEntity category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "课程分类不存在");
        }
        return category;
    }

    private CourseCategoryVO toVO(CourseCategoryEntity category) {
        return new CourseCategoryVO(
                String.valueOf(category.getId()),
                category.getName(),
                category.getSortOrder(),
                category.getEnabled() != null && category.getEnabled() == 1,
                category.getVersion());
    }
}
