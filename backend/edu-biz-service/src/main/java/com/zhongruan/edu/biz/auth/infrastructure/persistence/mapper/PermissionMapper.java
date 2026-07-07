package com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.PermissionEntity;
import java.util.Set;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PermissionMapper extends BaseMapper<PermissionEntity> {
    @Select("""
            SELECT DISTINCT p.permission_code
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id AND rp.deleted = 0
            JOIN sys_user_role ur ON ur.role_id = rp.role_id AND ur.deleted = 0
            JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.enabled = 1
            WHERE ur.user_id = #{userId} AND p.deleted = 0 AND p.enabled = 1
            ORDER BY p.permission_code
            """)
    Set<String> findPermissionCodesByUserId(@Param("userId") Long userId);
}

