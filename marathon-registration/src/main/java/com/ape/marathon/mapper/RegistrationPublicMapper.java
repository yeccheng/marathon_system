package com.ape.marathon.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 公开报名页查询：共享库下保留与赛事/成绩的 JOIN（演示可靠性）
 */
@Mapper
public interface RegistrationPublicMapper {

    @Select("SELECT id, name, gender, phone FROM runner WHERE phone = #{phone} LIMIT 1")
    Map<String, Object> selectRunnerByPhone(String phone);

    @Select("SELECT r.id AS registration_id, r.event_id, r.runner_id, r.bib_number, "
            + "r.shirt_size, r.order_no, r.register_time, r.pay_status, r.pay_channel, r.pay_time, r.pay_voucher, r.status, "
            + "r.refund_status, r.refund_no, r.refund_amount, r.refund_time, "
            + "e.event_name, e.event_date, e.distance_km, e.start_location, e.cancel_rule, e.entry_fee "
            + "FROM registration r JOIN marathon_event e ON r.event_id = e.id "
            + "WHERE r.runner_id = #{runnerId} ORDER BY r.register_time DESC")
    List<Map<String, Object>> selectMyRegistrations(Long runnerId);

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
}
