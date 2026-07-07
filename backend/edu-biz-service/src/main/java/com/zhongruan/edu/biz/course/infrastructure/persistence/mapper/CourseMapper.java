package com.zhongruan.edu.biz.course.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<CourseEntity> {}
