package com.xueying.jobapplicationtracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xueying.jobapplicationtracker.entity.ApplicationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for application rows.
 */
@Mapper
public interface ApplicationMapper extends BaseMapper<ApplicationEntity> {
}

