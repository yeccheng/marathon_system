package com.ape.marathon.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SmsLogMapper {

    @Insert("INSERT INTO sms_log (phone, scene, content, provider, success, provider_msg) "
            + "VALUES (#{phone}, #{scene}, #{content}, #{provider}, #{success}, #{providerMsg})")
    int insert(@Param("phone") String phone,
               @Param("scene") String scene,
               @Param("content") String content,
               @Param("provider") String provider,
               @Param("success") int success,
               @Param("providerMsg") String providerMsg);
}
