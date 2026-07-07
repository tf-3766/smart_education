package com.zhongruan.edu.biz.course.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEnrollmentEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseEnrollmentMapper extends BaseMapper<CourseEnrollmentEntity> {}
