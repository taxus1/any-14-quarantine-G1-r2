package com.somepro.infrastructure.persistence.farm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.farm.po.FarmPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 养殖场 Mapper（基础设施层）。阻塞 JDBC，只能在 boundedElastic 线程上调用。
 */
@Mapper
public interface FarmMapper extends BaseMapper<FarmPO> {

    /**
     * 取某年度已有场编号中的最大者（用于顺序号 +1）。
     *
     * 刻意不加 del_flag 条件：已软删的场也占号，编号一经分配永不复用，才能保证
     * 「同一个号不能落到两家场头上」。这是自定义 @Select，MyBatis-Plus 不会自动
     * 给它拼逻辑删除条件，正好符合需求。
     *
     * prefix 必须先经 escapeLike 转义（前缀里有下划线通配），SQL 里声明 ESCAPE。
     */
    @Select("SELECT MAX(farm_no) FROM t_farm WHERE farm_no LIKE CONCAT(#{prefix}, '%') ESCAPE '\\\\'")
    String selectMaxFarmNo(@Param("prefix") String prefix);
}
