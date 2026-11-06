package com.ape.marathon.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 公开成绩/排名：共享库下保留 JOIN SQL
 */
@Mapper
public interface ResultPublicMapper {

    @Select("SELECT COUNT(*) AS finishers,"
            + " MIN(re.net_time) AS best_net,"
            + " ROUND(AVG(re.net_time)) AS avg_net,"
            + " MAX(re.net_time) AS slowest_net,"
            + " SUM(CASE WHEN re.net_time < 10800 THEN 1 ELSE 0 END) AS under_3h,"
            + " SUM(CASE WHEN re.net_time >= 10800 AND re.net_time < 14400 THEN 1 ELSE 0 END) AS h3_to_4,"
            + " SUM(CASE WHEN re.net_time >= 14400 AND re.net_time < 18000 THEN 1 ELSE 0 END) AS h4_to_5,"
            + " SUM(CASE WHEN re.net_time >= 18000 THEN 1 ELSE 0 END) AS over_5h,"
            + " SUM(CASE WHEN ru.gender = 1 THEN 1 ELSE 0 END) AS male_count,"
            + " SUM(CASE WHEN ru.gender = 2 THEN 1 ELSE 0 END) AS female_count"
            + " FROM result re"
            + " JOIN registration rg ON re.registration_id = rg.id"
            + " JOIN runner ru ON rg.runner_id = ru.id"
            + " WHERE rg.event_id = #{eventId} AND re.finish_status = 1")
    Map<String, Object> selectEventFinishStats(@Param("eventId") Long eventId);

    @Select({"<script>",
            "SELECT t.overall_rank AS overallRank, t.gender_rank AS genderRank, t.age_rank AS ageRank,",
            "       t.age_group AS ageGroup, t.name AS runnerName, t.gender AS gender,",
            "       t.bib_number AS bibNumber, t.province_code AS provinceCode,",
            "       t.chip_time AS chipTime, t.chip_time_str AS chipTimeStr,",
            "       t.net_time AS netTime, t.net_time_str AS netTimeStr,",
            "       t.distance_km AS distanceKm",
            "FROM (",
            "  SELECT",
            "    ROW_NUMBER() OVER (ORDER BY re.net_time ASC) AS overall_rank,",
            "    ROW_NUMBER() OVER (PARTITION BY ru.gender ORDER BY re.net_time ASC) AS gender_rank,",
            "    ROW_NUMBER() OVER (PARTITION BY (",
            "      CASE WHEN TIMESTAMPDIFF(YEAR, ru.birth_date, e.event_date) &lt;= 29 THEN 1",
            "           WHEN TIMESTAMPDIFF(YEAR, ru.birth_date, e.event_date) BETWEEN 30 AND 39 THEN 2",
            "           WHEN TIMESTAMPDIFF(YEAR, ru.birth_date, e.event_date) BETWEEN 40 AND 49 THEN 3",
            "           ELSE 4 END",
            "    ) ORDER BY re.net_time ASC) AS age_rank,",
            "    CASE WHEN TIMESTAMPDIFF(YEAR, ru.birth_date, e.event_date) &lt;= 29 THEN 'U29'",
            "         WHEN TIMESTAMPDIFF(YEAR, ru.birth_date, e.event_date) BETWEEN 30 AND 39 THEN 'M30'",
            "         WHEN TIMESTAMPDIFF(YEAR, ru.birth_date, e.event_date) BETWEEN 40 AND 49 THEN 'M40'",
            "         ELSE 'M50+' END AS age_group,",
            "    ru.name AS name, ru.gender AS gender, rg.bib_number AS bib_number,",
            "    LEFT(ru.id_card, 2) AS province_code,",
            "    re.chip_time AS chip_time, SEC_TO_TIME(re.chip_time) AS chip_time_str,",
            "    re.net_time AS net_time, SEC_TO_TIME(re.net_time) AS net_time_str,",
            "    e.distance_km AS distance_km",
            "  FROM result re",
            "  JOIN registration rg ON re.registration_id = rg.id",
            "  JOIN runner ru ON rg.runner_id = ru.id",
            "  JOIN marathon_event e ON rg.event_id = e.id",
            "  WHERE rg.event_id = #{eventId} AND re.finish_status = 1",
            ") t",
            "WHERE 1=1",
            "<if test='bibNumber != null and bibNumber != \"\"'>AND t.bib_number LIKE CONCAT('%', #{bibNumber}, '%')</if>",
            "<if test='bibPrefix != null and bibPrefix != \"\"'>AND UPPER(t.bib_number) LIKE CONCAT(UPPER(#{bibPrefix}), '%')</if>",
            "<if test='province != null and province != \"\"'>AND t.province_code = #{province}</if>",
            "<if test='name != null and name != \"\"'>AND t.name LIKE CONCAT('%', #{name}, '%')</if>",
            "<if test='keyword != null and keyword != \"\"'>AND (t.name LIKE CONCAT('%', #{keyword}, '%') OR t.bib_number LIKE CONCAT('%', #{keyword}, '%'))</if>",
            "ORDER BY t.overall_rank ASC",
            "LIMIT 200",
            "</script>"})
    List<Map<String, Object>> selectPublicRank(@Param("eventId") Long eventId,
                                              @Param("bibNumber") String bibNumber,
                                              @Param("bibPrefix") String bibPrefix,
                                              @Param("province") String province,
                                              @Param("name") String name,
                                              @Param("keyword") String keyword);
}
