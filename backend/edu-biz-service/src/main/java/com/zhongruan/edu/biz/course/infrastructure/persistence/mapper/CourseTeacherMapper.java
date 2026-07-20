package com.zhongruan.edu.biz.course.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseTeacherEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CourseTeacherMapper extends BaseMapper<CourseTeacherEntity> {
    @Select("""
            SELECT *
            FROM edu_course_teacher
            WHERE course_id = #{courseId} AND teacher_id = #{teacherId}
            LIMIT 1
            """)
    CourseTeacherEntity findIncludingDeleted(
            @Param("courseId") Long courseId, @Param("teacherId") Long teacherId);

    @Update("""
            UPDATE edu_course_teacher
            SET role = 'COLLABORATOR', status = 'PENDING', deleted = 0,
                updated_at = CURRENT_TIMESTAMP, updated_by = #{operatorId}, version = version + 1
            WHERE id = #{relationId} AND deleted = 1
            """)
    int restoreInvitation(@Param("relationId") Long relationId, @Param("operatorId") Long operatorId);
}
