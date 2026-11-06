package com.ape.marathon.auth.mapper;

import com.ape.marathon.auth.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 管理员 Mapper：登录校验
 */
@Mapper
public interface AdminMapper {

    @Select("SELECT * FROM admin WHERE username = #{username}")
    Admin selectByUsername(String username);
}
