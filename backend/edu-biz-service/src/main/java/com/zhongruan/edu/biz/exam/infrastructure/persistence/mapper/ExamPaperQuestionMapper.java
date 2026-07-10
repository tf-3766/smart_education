package com.zhongruan.edu.biz.exam.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhongruan.edu.biz.exam.infrastructure.persistence.entity.ExamPaperQuestionEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamPaperQuestionMapper extends BaseMapper<ExamPaperQuestionEntity> {
    @Delete("DELETE FROM edu_exam_paper_question WHERE paper_id = #{paperId}")
    int deletePhysicalByPaperId(Long paperId);
}
