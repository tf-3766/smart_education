package com.zhongruan.edu.biz.storage.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhongruan.edu.biz.storage.infrastructure.persistence.entity.StoredFileEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StoredFileMapper extends BaseMapper<StoredFileEntity> {}
