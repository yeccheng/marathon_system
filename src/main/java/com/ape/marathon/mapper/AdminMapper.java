package com.ape.marathon.mapper;

import com.ape.marathon.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 管理员 Mapper：登录模块使用
 */
@Mapper
public interface AdminMapper {

    /**
     * 按账号查询管理员（登录校验）
     */
    @Select("SELECT * FROM admin WHERE username = #{username}")
    Admin selectByUsername(String username);
}