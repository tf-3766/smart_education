package com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
    @Select("""
            SELECT DISTINCT u.*
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
            JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.enabled = 1
            WHERE u.deleted = 0 AND u.user_status = 'ENABLED' AND r.role_code = 'TEACHER'
            ORDER BY u.display_name, u.id
            """)
    List<UserEntity> findEnabledTeachers();
}
