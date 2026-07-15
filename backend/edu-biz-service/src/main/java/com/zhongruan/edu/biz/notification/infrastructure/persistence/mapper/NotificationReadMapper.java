package com.zhongruan.edu.biz.notification.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhongruan.edu.biz.notification.infrastructure.persistence.entity.NotificationReadEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationReadMapper extends BaseMapper<NotificationReadEntity> {}
