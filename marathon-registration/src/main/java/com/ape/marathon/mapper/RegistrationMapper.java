package com.ape.marathon.mapper;

import com.ape.marathon.entity.Registration;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 报名 Mapper：赛事报名相关的增删改查
 */
@Mapper
public interface RegistrationMapper {

    /**
     * 新增报名记录（在 Service 中已校验名额与防重复）
     */
    @Insert("INSERT INTO registration (event_id, runner_id, bib_number, shirt_size, order_no, pay_status, status) "
            + "VALUES (#{eventId}, #{runnerId}, #{bibNumber}, #{shirtSize}, #{orderNo}, #{payStatus}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Registration registration);

    /**
     * 按赛事ID + 选手ID 查询报名记录（用于防重复报名校验）
     */
    @Select("SELECT * FROM registration WHERE event_id = #{eventId} AND runner_id = #{runnerId} LIMIT 1")
    Registration selectByEventAndRunner(@Param("eventId") Long eventId, @Param("runnerId") Long runnerId);

    /**
     * 取消报名：将报名状态改为 2（已取消）
     */
    @Update("UPDATE registration SET status = 2 WHERE id = #{id}")
    int cancel(Long id);

    /**
     * 分配号码布：更新报名记录的号码布编号
     */
    @Update("UPDATE registration SET bib_number = #{bibNumber} WHERE id = #{id}")
    int updateBib(@Param("id") Long id, @Param("bibNumber") String bibNumber);

    /** 确认支付：未支付 → 已支付，写入渠道、时间与凭证 */
    @Update("UPDATE registration SET pay_status = 1, pay_channel = #{payChannel}, pay_time = NOW(), pay_voucher = #{payVoucher} WHERE id = #{id}")
    int markPaid(@Param("id") Long id, @Param("payVoucher") String payVoucher, @Param("payChannel") String payChannel);

    /** 退款成功：pay_status=2，写入退款单号与金额 */
    @Update("UPDATE registration SET pay_status = 2, refund_status = 1, refund_no = #{refundNo},"
            + " refund_amount = #{refundAmount}, refund_time = NOW(), status = 2 WHERE id = #{id}")
    int markRefunded(@Param("id") Long id,
                     @Param("refundNo") String refundNo,
                     @Param("refundAmount") java.math.BigDecimal refundAmount);

    @Select("SELECT * FROM registration WHERE id = #{id}")
    Registration selectById(Long id);

    /**
     * 分页查询报名名单：可按赛事ID、选手姓名筛选，关联选手和赛事信息
     * 返回 Map：含报名字段 + runner(姓名/性别/电话) + event(赛事名称)
     */
    @Select({"<script>",
            "SELECT rg.id, rg.event_id, rg.runner_id, rg.bib_number, rg.shirt_size, rg.order_no,"
            + " rg.register_time, rg.pay_status, rg.pay_channel, rg.pay_time, rg.pay_voucher, rg.status,"
            + " rg.refund_status, rg.refund_no, rg.refund_amount, rg.refund_time,"
            + " r.name AS runner_name, r.gender AS runner_gender, r.phone AS runner_phone,"
            + " e.event_name AS event_name",
            "FROM registration rg",
            "JOIN runner r ON rg.runner_id = r.id",
            "JOIN marathon_event e ON rg.event_id = e.id",
            "WHERE 1=1",
            "<if test='eventId != null'>AND rg.event_id = #{eventId}</if>",
            "<if test='name != null and name != \"\"'>AND r.name LIKE CONCAT('%', #{name}, '%')</if>",
            "ORDER BY rg.register_time DESC",
            "LIMIT #{offset}, #{pageSize}",
            "</script>"})
    List<Map<String, Object>> selectPage(@Param("eventId") Long eventId,
                                         @Param("name") String name,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    /**
     * 统计满足条件的报名记录总数
     */
    @Select({"<script>",
            "SELECT COUNT(*) FROM registration rg",
            "JOIN runner r ON rg.runner_id = r.id",
            "WHERE 1=1",
            "<if test='eventId != null'>AND rg.event_id = #{eventId}</if>",
            "<if test='name != null and name != \"\"'>AND r.name LIKE CONCAT('%', #{name}, '%')</if>",
            "</script>"})
    long countForPage(@Param("eventId") Long eventId, @Param("name") String name);

    /**
     * 查询单条报名详情：附带选手信息与赛事简要信息
     */
    @Select("SELECT rg.id, rg.event_id, rg.runner_id, rg.bib_number, rg.shirt_size, rg.order_no,"
            + " rg.register_time, rg.pay_status, rg.pay_channel, rg.pay_time, rg.pay_voucher, rg.status,"
            + " rg.refund_status, rg.refund_no, rg.refund_amount, rg.refund_time,"
            + " r.id AS runner_id, r.name AS runner_name, r.gender AS runner_gender, r.phone AS runner_phone,"
            + " e.id AS event_id, e.event_name AS event_name, e.event_date AS event_date,"
            + " e.cancel_rule AS cancel_rule, e.entry_fee AS entry_fee"
            + " FROM registration rg"
            + " JOIN runner r ON rg.runner_id = r.id"
            + " JOIN marathon_event e ON rg.event_id = e.id"
            + " WHERE rg.id = #{id}")
    Map<String, Object> selectDetailById(Long id);

    /**
     * 统计某赛事已报名的有效记录数（status=1，用于报名名额判断）
     */
    @Select("SELECT COUNT(*) FROM registration WHERE event_id = #{eventId} AND status = 1")
    long countActiveByEvent(Long eventId);

    /**
     * 统计某赛事全部报名记录数（含退赛/取消，用于统计看板）
     */
    @Select("SELECT COUNT(*) FROM registration WHERE event_id = #{eventId}")
    long countByEvent(Long eventId);

    /**
     * 按性别统计某赛事的报名人数（status=1，用于男女占比）
     */
    @Select("SELECT COUNT(*) FROM registration rg JOIN runner r ON rg.runner_id = r.id"
            + " WHERE rg.event_id = #{eventId} AND rg.status = 1 AND r.gender = #{gender}")
    long countGenderByEvent(@Param("eventId") Long eventId, @Param("gender") Integer gender);

    /**
     * 统计某赛事退赛人数（status=3）
     */
    @Select("SELECT COUNT(*) FROM registration WHERE event_id = #{eventId} AND status = 3")
    long countRetireByEvent(Long eventId);

    /** 某选手是否有报名记录（删除选手前校验） */
    @Select("SELECT COUNT(*) FROM registration WHERE runner_id = #{runnerId}")
    long countByRunner(@Param("runnerId") Long runnerId);

    /** 某赛事是否存在任意报名（含取消） */
    @Select("SELECT COUNT(*) FROM registration WHERE event_id = #{eventId}")
    long countAnyByEvent(@Param("eventId") Long eventId);

    /**
     * 导出用：按赛事查询全部报名名单（不分页），关联选手与赛事名称
     */
    @Select("SELECT rg.id, rg.bib_number, rg.register_time, rg.pay_status, rg.status,"
            + " r.name AS runner_name, r.gender AS runner_gender, r.phone AS runner_phone,"
            + " e.event_name AS event_name"
            + " FROM registration rg"
            + " JOIN runner r ON rg.runner_id = r.id"
            + " JOIN marathon_event e ON rg.event_id = e.id"
            + " WHERE rg.event_id = #{eventId}"
            + " ORDER BY rg.register_time DESC")
    List<Map<String, Object>> selectExportList(@Param("eventId") Long eventId);
}