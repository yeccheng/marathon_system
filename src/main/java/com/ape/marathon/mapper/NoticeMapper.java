package com.ape.marathon.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface NoticeMapper {

    @Insert("INSERT INTO runner_notice (runner_id, title, content) VALUES (#{runnerId}, #{title}, #{content})")
    int insert(@Param("runnerId") Long runnerId,
               @Param("title") String title,
               @Param("content") String content);

    @Select("SELECT id, runner_id, title, content, created_at, read_flag "
            + "FROM runner_notice WHERE runner_id = #{runnerId} ORDER BY id DESC LIMIT 50")
    List<Map<String, Object>> listByRunner(Long runnerId);

    @Update("UPDATE runner_notice SET read_flag = 1 WHERE id = #{id} AND runner_id = #{runnerId}")
    int markRead(@Param("id") Long id, @Param("runnerId") Long runnerId);
}
