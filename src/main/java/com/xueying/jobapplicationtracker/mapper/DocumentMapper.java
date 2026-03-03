package com.xueying.jobapplicationtracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xueying.jobapplicationtracker.entity.DocumentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for document metadata rows.
 */
@Mapper
public interface DocumentMapper extends BaseMapper<DocumentEntity> {
}

