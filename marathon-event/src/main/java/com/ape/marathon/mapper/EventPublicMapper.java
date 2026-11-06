package com.ape.marathon.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 公开赛事列表：共享库下用子查询统计报名/完赛人数（演示稳定性优先）
 */
@Mapper
public interface EventPublicMapper {

    @Select("SELECT e.id, e.event_name, e.event_type, e.event_date, e.start_location, e.distance_km, "
            + "e.quota, e.entry_fee, e.status, e.signup_start, e.signup_end, e.gun_time, "
            + "(SELECT COUNT(*) FROM registration r WHERE r.event_id = e.id AND r.status = 1) AS active_count, "
            + "(SELECT COUNT(*) FROM result re JOIN registration rg ON re.registration_id = rg.id "
            + "  WHERE rg.event_id = e.id AND re.finish_status = 1) AS finisher_count "
            + "FROM marathon_event e WHERE e.status = 0 ORDER BY e.signup_start DESC, e.id DESC")
    List<Map<String, Object>> selectPublicEvents();

    @Select("SELECT e.id, e.event_name, e.event_type, e.event_date, e.start_location, e.distance_km, "
            + "e.quota, e.entry_fee, e.status, e.signup_start, e.signup_end, e.gun_time, "
            + "(SELECT COUNT(*) FROM registration r WHERE r.event_id = e.id AND r.status = 1) AS active_count, "
            + "(SELECT COUNT(*) FROM result re JOIN registration rg ON re.registration_id = rg.id "
            + "  WHERE rg.event_id = e.id AND re.finish_status = 1) AS finisher_count "
            + "FROM marathon_event e ORDER BY e.event_date DESC, e.id DESC")
    List<Map<String, Object>> selectAllEvents();

    @Select("SELECT COUNT(*) FROM registration WHERE event_id = #{eventId} AND status = 1")
    int countActiveRegs(@Param("eventId") Long eventId);

    @Select("SELECT COUNT(*) FROM result re JOIN registration rg ON re.registration_id = rg.id "
            + "WHERE rg.event_id = #{eventId} AND re.finish_status = 1")
    int countFinishers(@Param("eventId") Long eventId);
}
