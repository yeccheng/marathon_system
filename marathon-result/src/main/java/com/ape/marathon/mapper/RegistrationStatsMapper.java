package com.ape.marathon.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 统计用报名计数（共享库，补齐 Feign 未覆盖的性别/退赛维度）
 */
@Mapper
public interface RegistrationStatsMapper {

    @Select("SELECT COUNT(*) FROM registration rg JOIN runner r ON rg.runner_id = r.id"
            + " WHERE rg.event_id = #{eventId} AND rg.status = 1 AND r.gender = #{gender}")
    long countGenderByEvent(@Param("eventId") Long eventId, @Param("gender") Integer gender);

    @Select("SELECT COUNT(*) FROM registration WHERE event_id = #{eventId} AND status = 3")
    long countRetireByEvent(Long eventId);
}
