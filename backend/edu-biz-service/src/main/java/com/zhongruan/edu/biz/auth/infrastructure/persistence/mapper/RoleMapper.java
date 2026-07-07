package com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.RoleEntity;
import java.util.Set;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {
    @Select("""
            SELECT r.role_code
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id AND ur.deleted = 0
            WHERE ur.user_id = #{userId} AND r.deleted = 0 AND r.enabled = 1
            ORDER BY r.id
            """)
    Set<String> findRoleCodesByUserId(@Param("userId") Long userId);
}

