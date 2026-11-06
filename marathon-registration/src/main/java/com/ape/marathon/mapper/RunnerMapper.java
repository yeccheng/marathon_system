package com.ape.marathon.mapper;

import com.ape.marathon.entity.Runner;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 选手 Mapper：选手档案的增删改查
 */
@Mapper
public interface RunnerMapper {

    /**
     * 新增选手档案
     * @Options(useGeneratedKeys=true)：回填自增 id
     */
    @Insert("INSERT INTO runner (name, gender, id_card, phone, birth_date, emergency_contact) "
            + "VALUES (#{name}, #{gender}, #{idCard}, #{phone}, #{birthDate}, #{emergencyContact})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Runner runner);

    /**
     * 修改选手档案：仅更新非 null 字段
     */
    @Update({"<script>",
            "UPDATE runner",
            "<set>",
            "<if test='name != null'>name = #{name},</if>",
            "<if test='gender != null'>gender = #{gender},</if>",
            "<if test='idCard != null'>id_card = #{idCard},</if>",
            "<if test='phone != null'>phone = #{phone},</if>",
            "<if test='birthDate != null'>birth_date = #{birthDate},</if>",
            "<if test='emergencyContact != null'>emergency_contact = #{emergencyContact},</if>",
            "</set>",
            "WHERE id = #{id}",
            "</script>"})
    int updateById(Runner runner);

    /**
     * 分页查询选手列表：支持姓名、手机号模糊检索
     */
    @Select({"<script>",
            "SELECT id, name, gender, id_card, phone, birth_date, emergency_contact, create_time",
            "FROM runner",
            "WHERE 1=1",
            "<if test='name != null and name != \"\"'>AND name LIKE CONCAT('%', #{name}, '%')</if>",
            "<if test='phone != null and phone != \"\"'>AND phone LIKE CONCAT('%', #{phone}, '%')</if>",
            "ORDER BY create_time DESC",
            "LIMIT #{offset}, #{pageSize}",
            "</script>"})
    List<Runner> selectPage(@Param("name") String name,
                            @Param("phone") String phone,
                            @Param("offset") int offset,
                            @Param("pageSize") int pageSize);

    /**
     * 统计满足条件的选手总数
     */
    @Select({"<script>",
            "SELECT COUNT(*) FROM runner",
            "WHERE 1=1",
            "<if test='name != null and name != \"\"'>AND name LIKE CONCAT('%', #{name}, '%')</if>",
            "<if test='phone != null and phone != \"\"'>AND phone LIKE CONCAT('%', #{phone}, '%')</if>",
            "</script>"})
    long countForPage(@Param("name") String name, @Param("phone") String phone);

    /**
     * 根据 id 查询选手详情
     */
    @Select("SELECT id, name, gender, id_card, phone, birth_date, emergency_contact, create_time "
            + "FROM runner WHERE id = #{id}")
    Runner selectById(Long id);

    /** 按手机号查选手（公开报名复用档案） */
    @Select("SELECT id, name, gender, id_card, phone, birth_date, emergency_contact, create_time "
            + "FROM runner WHERE phone = #{phone} LIMIT 1")
    Runner selectByPhone(String phone);

    /** 按身份证查选手 */
    @Select("SELECT id, name, gender, id_card, phone, birth_date, emergency_contact, create_time "
            + "FROM runner WHERE id_card = #{idCard} LIMIT 1")
    Runner selectByIdCard(String idCard);

    /**
     * 删除选手（调用前需校验无报名记录）
     */
    @Delete("DELETE FROM runner WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 统计某选手的报名记录数（用于删除前校验）
     */
    @Select("SELECT COUNT(*) FROM registration WHERE runner_id = #{runnerId}")
    long countRegistrationByRunnerId(Long runnerId);
}