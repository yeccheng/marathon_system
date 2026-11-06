package com.ape.marathon.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface PaymentLogMapper {

    @Insert("INSERT INTO payment_log (registration_id, order_no, action, channel, amount, voucher_no, remark) "
            + "VALUES (#{registrationId}, #{orderNo}, #{action}, #{channel}, #{amount}, #{voucherNo}, #{remark})")
    int insert(@Param("registrationId") Long registrationId,
               @Param("orderNo") String orderNo,
               @Param("action") String action,
               @Param("channel") String channel,
               @Param("amount") BigDecimal amount,
               @Param("voucherNo") String voucherNo,
               @Param("remark") String remark);
}
