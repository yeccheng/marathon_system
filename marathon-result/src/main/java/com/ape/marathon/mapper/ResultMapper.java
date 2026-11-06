package com.ape.marathon.mapper;

import com.ape.marathon.entity.Result;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 成绩 Mapper：成绩录入、排名、统计
 * 注意：rank 是 MySQL 8 保留字，作为别名时用反引号 `rank`
 */
@Mapper
public interface ResultMapper {

    /**
     * 录入成绩（调用前校验：一条报名记录只能录一次）
     * @Options(useGeneratedKeys=true)：回填自增 id
     */
    @Insert("INSERT INTO result (registration_id, chip_time, net_time, finish_status, finish_time) "
            + "VALUES (#{registrationId}, #{chipTime}, #{netTime}, #{finishStatus}, #{finishTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Result result);

    /**
     * 修改成绩：仅更新非 null 字段
     */
    @Update({"<script>",
            "UPDATE result",
            "<set>",
            "<if test='chipTime != null'>chip_time = #{chipTime},</if>",
            "<if test='netTime != null'>net_time = #{netTime},</if>",
            "<if test='finishStatus != null'>finish_status = #{finishStatus},</if>",
            "<if test='finishTime != null'>finish_time = #{finishTime},</if>",
            "</set>",
            "WHERE id = #{id}",
            "</script>"})
    int updateById(Result result);

    /**
     * 按报名记录ID查询成绩（用于录入前防重校验）
     */
    @Select("SELECT * FROM result WHERE registration_id = #{registrationId} LIMIT 1")
    Result selectByRegistration(Long registrationId);

    /**
     * 按成绩ID查询（修改成绩后定位所属赛事，用于清排名缓存）
     */
    @Select("SELECT * FROM result WHERE id = #{id} LIMIT 1")
    Result selectById(Long id);

    /**
     * 赛事总排名：所有完赛选手，按净成绩升序（使用 MySQL8 窗口函数生成 rank）
     * 返回 Map：rank, name, bib_number, gender, net_time, net_time_str
     */
    @Select("SELECT ROW_NUMBER() OVER (ORDER BY re.net_time ASC) AS `rank`,"
            + " ru.name AS name, rg.bib_number AS bib_number, ru.gender AS gender,"
            + " re.net_time AS net_time, SEC_TO_TIME(re.net_time) AS net_time_str"
            + " FROM result re"
            + " JOIN registration rg ON re.registration_id = rg.id"
            + " JOIN runner ru ON rg.runner_id = ru.id"
            + " WHERE rg.event_id = #{eventId} AND re.finish_status = 1"
            + " ORDER BY re.net_time ASC"
            + " LIMIT #{offset}, #{pageSize}")
    List<Map<String, Object>> eventRanking(@Param("eventId") Long eventId,
                                           @Param("offset") int offset,
                                           @Param("pageSize") int pageSize);

    /**
     * 按性别分组排名（gender 传入 1男 或 2女）
     */
    @Select("SELECT ROW_NUMBER() OVER (ORDER BY re.net_time ASC) AS `rank`,"
            + " ru.name AS name, rg.bib_number AS bib_number, ru.gender AS gender,"
            + " re.net_time AS net_time, SEC_TO_TIME(re.net_time) AS net_time_str"
            + " FROM result re"
            + " JOIN registration rg ON re.registration_id = rg.id"
            + " JOIN runner ru ON rg.runner_id = ru.id"
            + " WHERE rg.event_id = #{eventId} AND re.finish_status = 1 AND ru.gender = #{gender}"
            + " ORDER BY re.net_time ASC"
            + " LIMIT #{offset}, #{pageSize}")
    List<Map<String, Object>> genderRanking(@Param("eventId") Long eventId,
                                            @Param("gender") Integer gender,
                                            @Param("offset") int offset,
                                            @Param("pageSize") int pageSize);

    /**
     * 年龄分组排名：按年龄段条件（ageCondition 为 SQL 片段，如 age <= 29）
     * age 通过出生日期动态计算
     */
    @Select("SELECT ROW_NUMBER() OVER (ORDER BY re.net_time ASC) AS `rank`,"
            + " ru.name AS name, rg.bib_number AS bib_number, ru.gender AS gender,"
            + " TIMESTAMPDIFF(YEAR, ru.birth_date, CURDATE()) AS age,"
            + " re.net_time AS net_time, SEC_TO_TIME(re.net_time) AS net_time_str"
            + " FROM result re"
            + " JOIN registration rg ON re.registration_id = rg.id"
            + " JOIN runner ru ON rg.runner_id = ru.id"
            + " WHERE rg.event_id = #{eventId} AND re.finish_status = 1 AND ${ageCondition}"
            + " ORDER BY re.net_time ASC"
            + " LIMIT #{offset}, #{pageSize}")
    List<Map<String, Object>> ageRanking(@Param("eventId") Long eventId,
                                         @Param("ageCondition") String ageCondition,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    /**
     * 分页查询成绩记录：可按赛事ID筛选，关联选手姓名与号码布
     */
    @Select({"<script>",
            "SELECT re.id, re.registration_id, re.chip_time, re.net_time, re.finish_status, re.finish_time,"
            + " ru.name AS runner_name, rg.bib_number AS bib_number",
            "FROM result re",
            "JOIN registration rg ON re.registration_id = rg.id",
            "JOIN runner ru ON rg.runner_id = ru.id",
            "WHERE 1=1",
            "<if test='eventId != null'>AND rg.event_id = #{eventId}</if>",
            "ORDER BY re.net_time ASC",
            "LIMIT #{offset}, #{pageSize}",
            "</script>"})
    List<Map<String, Object>> selectPage(@Param("eventId") Long eventId,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    /**
     * 统计成绩记录总数
     */
    @Select({"<script>",
            "SELECT COUNT(*) FROM result re JOIN registration rg ON re.registration_id = rg.id",
            "WHERE 1=1",
            "<if test='eventId != null'>AND rg.event_id = #{eventId}</if>",
            "</script>"})
    long countForPage(@Param("eventId") Long eventId);

    /**
     * 统计某赛事完赛人数（finish_status=1）
     */
    @Select("SELECT COUNT(*) FROM result re JOIN registration rg ON re.registration_id = rg.id"
            + " WHERE rg.event_id = #{eventId} AND re.finish_status = 1")
    long countFinishedByEvent(Long eventId);

    /**
     * 某赛事平均净成绩（秒，仅统计完赛选手）
     */
    @Select("SELECT IFNULL(AVG(re.net_time), 0) FROM result re JOIN registration rg ON re.registration_id = rg.id"
            + " WHERE rg.event_id = #{eventId} AND re.finish_status = 1")
    Long avgNetTimeByEvent(Long eventId);

    /**
     * 导出用：赛事成绩排名（不分页），完赛选手按净成绩升序
     * 返回：rank, name, bib_number, gender, net_time, finish_status
     */
    @Select("SELECT ROW_NUMBER() OVER (ORDER BY re.net_time ASC) AS `rank`,"
            + " ru.name AS name, rg.bib_number AS bib_number, ru.gender AS gender,"
            + " re.net_time AS net_time, re.finish_status AS finish_status"
            + " FROM result re"
            + " JOIN registration rg ON re.registration_id = rg.id"
            + " JOIN runner ru ON rg.runner_id = ru.id"
            + " WHERE rg.event_id = #{eventId} AND re.finish_status = 1"
            + " ORDER BY re.net_time ASC")
    List<Map<String, Object>> selectExportRanking(@Param("eventId") Long eventId);
}