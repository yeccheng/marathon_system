package com.ape.marathon.mapper;

import com.ape.marathon.entity.MarathonEvent;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 赛事 Mapper：对应马拉松赛事的增删改查
 */
@Mapper
public interface MarathonEventMapper {

    String COLS = "id, event_name, event_type, event_date, start_location, distance_km, quota, registered_count, entry_fee, status, signup_start, signup_end, gun_time, cut_off_time, signup_rule, pickup_info, route_desc, aid_stations, route_profile, route_gpx, guide_json, cancel_rule, create_time";

    @Insert("INSERT INTO marathon_event (event_name, event_type, event_date, start_location, distance_km, quota, registered_count, entry_fee, status, signup_start, signup_end, gun_time, cut_off_time, signup_rule, pickup_info, route_desc) "
            + "VALUES (#{eventName}, #{eventType}, #{eventDate}, #{startLocation}, #{distanceKm}, #{quota}, #{registeredCount}, #{entryFee}, #{status}, #{signupStart}, #{signupEnd}, #{gunTime}, #{cutOffTime}, #{signupRule}, #{pickupInfo}, #{routeDesc})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MarathonEvent event);

    @Update({"<script>",
            "UPDATE marathon_event",
            "<set>",
            "<if test='eventName != null'>event_name = #{eventName},</if>",
            "<if test='eventType != null'>event_type = #{eventType},</if>",
            "<if test='eventDate != null'>event_date = #{eventDate},</if>",
            "<if test='startLocation != null'>start_location = #{startLocation},</if>",
            "<if test='distanceKm != null'>distance_km = #{distanceKm},</if>",
            "<if test='quota != null'>quota = #{quota},</if>",
            "<if test='entryFee != null'>entry_fee = #{entryFee},</if>",
            "<if test='status != null'>status = #{status},</if>",
            "<if test='signupStart != null'>signup_start = #{signupStart},</if>",
            "<if test='signupEnd != null'>signup_end = #{signupEnd},</if>",
            "<if test='gunTime != null'>gun_time = #{gunTime},</if>",
            "<if test='cutOffTime != null'>cut_off_time = #{cutOffTime},</if>",
            "<if test='signupRule != null'>signup_rule = #{signupRule},</if>",
            "<if test='pickupInfo != null'>pickup_info = #{pickupInfo},</if>",
            "<if test='routeDesc != null'>route_desc = #{routeDesc},</if>",
            "<if test='aidStations != null'>aid_stations = #{aidStations},</if>",
            "<if test='routeProfile != null'>route_profile = #{routeProfile},</if>",
            "<if test='routeGpx != null'>route_gpx = #{routeGpx},</if>",
            "<if test='guideJson != null'>guide_json = #{guideJson},</if>",
            "<if test='cancelRule != null'>cancel_rule = #{cancelRule},</if>",
            "</set>",
            "WHERE id = #{id}",
            "</script>"})
    int updateById(MarathonEvent event);

    @Update("UPDATE marathon_event SET registered_count = registered_count + 1 WHERE id = #{eventId}")
    int increaseRegisteredCount(Long eventId);

    @Update("UPDATE marathon_event SET registered_count = registered_count - 1 WHERE id = #{eventId} AND registered_count > 0")
    int decreaseRegisteredCount(Long eventId);

    /** 用有效报名数回写 registered_count，避免各页剩余名额不一致 */
    @Update("UPDATE marathon_event SET registered_count = "
            + "(SELECT COUNT(*) FROM registration r WHERE r.event_id = #{eventId} AND r.status = 1) "
            + "WHERE id = #{eventId}")
    int syncRegisteredCount(Long eventId);

    @Select({"<script>",
            "SELECT " + COLS,
            "FROM marathon_event",
            "WHERE 1=1",
            "<if test='eventName != null and eventName != \"\"'>AND event_name LIKE CONCAT('%', #{eventName}, '%')</if>",
            "<if test='eventType != null'>AND event_type = #{eventType}</if>",
            "<if test='status != null'>AND status = #{status}</if>",
            "<if test='startDate != null'>AND event_date &gt;= #{startDate}</if>",
            "<if test='endDate != null'>AND event_date &lt;= #{endDate}</if>",
            "ORDER BY event_date DESC",
            "LIMIT #{offset}, #{pageSize}",
            "</script>"})
    List<MarathonEvent> selectPage(@Param("eventName") String eventName,
                                   @Param("eventType") Integer eventType,
                                   @Param("status") Integer status,
                                   @Param("startDate") String startDate,
                                   @Param("endDate") String endDate,
                                   @Param("offset") int offset,
                                   @Param("pageSize") int pageSize);

    @Select({"<script>",
            "SELECT COUNT(*) FROM marathon_event",
            "WHERE 1=1",
            "<if test='eventName != null and eventName != \"\"'>AND event_name LIKE CONCAT('%', #{eventName}, '%')</if>",
            "<if test='eventType != null'>AND event_type = #{eventType}</if>",
            "<if test='status != null'>AND status = #{status}</if>",
            "<if test='startDate != null'>AND event_date &gt;= #{startDate}</if>",
            "<if test='endDate != null'>AND event_date &lt;= #{endDate}</if>",
            "</script>"})
    long countForPage(@Param("eventName") String eventName,
                      @Param("eventType") Integer eventType,
                      @Param("status") Integer status,
                      @Param("startDate") String startDate,
                      @Param("endDate") String endDate);

    @Select("SELECT " + COLS + " FROM marathon_event WHERE id = #{id}")
    MarathonEvent selectById(Long id);

    @Delete("DELETE FROM marathon_event WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM registration WHERE event_id = #{eventId}")
    long countRegistrationByEventId(Long eventId);
}