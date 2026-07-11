package com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {
    @Select("""
            SELECT COUNT(*)
            FROM sys_user_role
            WHERE user_id = #{userId} AND role_id = #{roleId} AND deleted = 0
            """)
    long countActiveRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Update("""
            UPDATE sys_user_role
            SET deleted = 0,
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{operatorId},
                version = version + 1
            WHERE user_id = #{userId} AND role_id = #{roleId} AND deleted = 1
            """)
    int restoreRole(
            @Param("userId") Long userId,
            @Param("roleId") Long roleId,
            @Param("operatorId") Long operatorId);

    @Update("""
            UPDATE sys_user_role
            SET deleted = 1,
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{operatorId},
                version = version + 1
            WHERE user_id = #{userId} AND role_id = #{roleId} AND deleted = 0
            """)
    int revokeRole(
            @Param("userId") Long userId,
            @Param("roleId") Long roleId,
            @Param("operatorId") Long operatorId);
}
