package com.ape.marathon.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 选手端公开查询 Mapper（不需要登录）
 * 注意：net_time_str / chip_time_str 不是表字段，须用 SEC_TO_TIME 计算
 */
@Mapper
public interface PublicMapper {

    /** 查正在报名中的赛事列表（给选手端首页）；active_count=有效报名实时人数 */
    @Select("SELECT e.id, e.event_name, e.event_type, e.event_date, e.start_location, e.distance_km, "
            + "e.quota, e.entry_fee, e.status, e.signup_start, e.signup_end, e.gun_time, "
            + "(SELECT COUNT(*) FROM registration r WHERE r.event_id = e.id AND r.status = 1) AS active_count, "
            + "(SELECT COUNT(*) FROM result re JOIN registration rg ON re.registration_id = rg.id "
            + "  WHERE rg.event_id = e.id AND re.finish_status = 1) AS finisher_count "
            + "FROM marathon_event e WHERE e.status = 0 ORDER BY e.signup_start DESC, e.id DESC")
    List<Map<String, Object>> selectPublicEvents();

    /** 全部赛事（含已截止/已结束，供查成绩与公开榜选赛） */
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

    /** 按手机号查选手 */
    @Select("SELECT id, name, gender, phone FROM runner WHERE phone = #{phone} LIMIT 1")
    Map<String, Object> selectRunnerByPhone(String phone);

    /** 赛事完赛成绩分布（公开） */
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

    /** 查某选手的所有报名记录（嵌套赛事信息） */
    @Select("SELECT r.id AS registration_id, r.event_id, r.runner_id, r.bib_number, "
            + "r.shirt_size, r.order_no, r.register_time, r.pay_status, r.pay_channel, r.pay_time, r.pay_voucher, r.status, "
            + "r.refund_status, r.refund_no, r.refund_amount, r.refund_time, "
            + "e.event_name, e.event_date, e.distance_km, e.start_location, e.cancel_rule, e.entry_fee "
            + "FROM registration r JOIN marathon_event e ON r.event_id = e.id "
            + "WHERE r.runner_id = #{runnerId} ORDER BY r.register_time DESC")
    List<Map<String, Object>> selectMyRegistrations(Long runnerId);

    /**
     * 某选手成绩 + 枪声 + 总/性别/年龄组名次（仅完赛有名次）
     */
    @Select("SELECT re.registration_id AS registration_id,"
            + " re.chip_time AS chip_time,"
            + " SEC_TO_TIME(re.chip_time) AS chip_time_str,"
            + " re.net_time AS net_time,"
            + " SEC_TO_TIME(re.net_time) AS net_time_str,"
            + " re.split_5k AS split_5k,"
            + " SEC_TO_TIME(re.split_5k) AS split_5k_str,"
            + " re.split_10k AS split_10k,"
            + " SEC_TO_TIME(re.split_10k) AS split_10k_str,"
            + " re.split_21k AS split_21k,"
            + " SEC_TO_TIME(re.split_21k) AS split_21k_str,"
            + " re.finish_status AS finish_status,"
            + " e.distance_km AS distance_km,"
            + " CASE"
            + "   WHEN TIMESTAMPDIFF(YEAR, ru.birth_date, e.event_date) <= 29 THEN 'U29'"
            + "   WHEN TIMESTAMPDIFF(YEAR, ru.birth_date, e.event_date) BETWEEN 30 AND 39 THEN 'M30'"
            + "   WHEN TIMESTAMPDIFF(YEAR, ru.birth_date, e.event_date) BETWEEN 40 AND 49 THEN 'M40'"
            + "   ELSE 'M50+'"
            + " END AS age_group,"
            + " IF(re.finish_status = 1,"
            + "   (SELECT COUNT(*) + 1 FROM result r2"
            + "     JOIN registration g2 ON r2.registration_id = g2.id"
            + "    WHERE g2.event_id = rg.event_id AND r2.finish_status = 1 AND r2.net_time < re.net_time),"
            + "   NULL) AS `rank`,"
            + " IF(re.finish_status = 1,"
            + "   (SELECT COUNT(*) + 1 FROM result r2"
            + "     JOIN registration g2 ON r2.registration_id = g2.id"
            + "     JOIN runner u2 ON g2.runner_id = u2.id"
            + "    WHERE g2.event_id = rg.event_id AND r2.finish_status = 1"
            + "      AND u2.gender = ru.gender AND r2.net_time < re.net_time),"
            + "   NULL) AS gender_rank,"
            + " IF(re.finish_status = 1,"
            + "   (SELECT COUNT(*) + 1 FROM result r2"
            + "     JOIN registration g2 ON r2.registration_id = g2.id"
            + "     JOIN runner u2 ON g2.runner_id = u2.id"
            + "     JOIN marathon_event e2 ON g2.event_id = e2.id"
            + "    WHERE g2.event_id = rg.event_id AND r2.finish_status = 1"
            + "      AND ("
            + "        CASE WHEN TIMESTAMPDIFF(YEAR, u2.birth_date, e2.event_date) <= 29 THEN 1"
            + "             WHEN TIMESTAMPDIFF(YEAR, u2.birth_date, e2.event_date) BETWEEN 30 AND 39 THEN 2"
            + "             WHEN TIMESTAMPDIFF(YEAR, u2.birth_date, e2.event_date) BETWEEN 40 AND 49 THEN 3"
            + "             ELSE 4 END"
            + "      ) = ("
            + "        CASE WHEN TIMESTAMPDIFF(YEAR, ru.birth_date, e.event_date) <= 29 THEN 1"
            + "             WHEN TIMESTAMPDIFF(YEAR, ru.birth_date, e.event_date) BETWEEN 30 AND 39 THEN 2"
            + "             WHEN TIMESTAMPDIFF(YEAR, ru.birth_date, e.event_date) BETWEEN 40 AND 49 THEN 3"
            + "             ELSE 4 END"
            + "      )"
            + "      AND r2.net_time < re.net_time),"
            + "   NULL) AS age_rank"
            + " FROM result re"
            + " JOIN registration rg ON re.registration_id = rg.id"
            + " JOIN runner ru ON rg.runner_id = ru.id"
            + " JOIN marathon_event e ON rg.event_id = e.id"
            + " WHERE rg.runner_id = #{runnerId}")
    List<Map<String, Object>> selectResultsWithRanksByRunnerId(@Param("runnerId") Long runnerId);

    /**
     * 赛事公开排名：总名次 + 性别名次 + 年龄组名次 + 枪声/净成绩
     */
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
