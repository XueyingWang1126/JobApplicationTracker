package com.xueying.jobapplicationtracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xueying.jobapplicationtracker.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * Mapper for app user credentials.
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT id, email, username, password, salt FROM app_users WHERE email = #{email} LIMIT 1")
    User findByEmail(String email);
}

