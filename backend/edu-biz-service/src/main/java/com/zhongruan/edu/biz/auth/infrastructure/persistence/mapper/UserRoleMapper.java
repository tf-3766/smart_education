package com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {}

