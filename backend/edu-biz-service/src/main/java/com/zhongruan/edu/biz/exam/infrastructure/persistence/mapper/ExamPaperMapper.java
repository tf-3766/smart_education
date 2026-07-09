package com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.ExamPaperEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamPaperMapper extends BaseMapper<ExamPaperEntity> {}
