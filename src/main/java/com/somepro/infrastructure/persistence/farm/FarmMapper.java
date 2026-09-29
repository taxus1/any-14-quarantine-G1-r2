package com.somepro.infrastructure.persistence.farm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.farm.po.FarmPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 养殖场档案 Mapper（基础设施层，阻塞 JDBC，只能在 boundedElastic 线程调用）。
 */
@Mapper
public interface FarmMapper extends BaseMapper<FarmPO> {

    /**
     * 取某编号前缀下的全部编号（含已软删记录），供应用层算出年内最大顺序号。
     *
     * 必须带上软删行：否则「FM-2026-0007 被注销」后再登记，会重新发 0007，撞唯一键/重号。
     * 这是手写 SQL，@TableLogic 不会自动追加 del_flag 条件，这里也刻意不加。
     *
     * prefix 已在 Java 侧转义 % / _，ESCAPE '\\' 让转义生效。
     */
    @Select("SELECT farm_no FROM t_farm WHERE farm_no LIKE #{prefix} ESCAPE '\\\\'")
    List<String> selectAllNosLike(@Param("prefix") String prefix);
}
