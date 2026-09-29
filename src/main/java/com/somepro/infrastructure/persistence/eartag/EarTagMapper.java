package com.somepro.infrastructure.persistence.eartag;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.eartag.po.EarTagPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 耳标 Mapper（基础设施层）。阻塞 JDBC，只能在 boundedElastic 线程上调用。
 */
@Mapper
public interface EarTagMapper extends BaseMapper<EarTagPO> {

    /**
     * 取某年度已有耳标号中的最大者（用于顺序号 +1）。
     *
     * 与场编号同理：不加 del_flag 条件，已缴销的耳标也占号，号码一经分配永不复用。
     * prefix 需先转义下划线通配，配套 ESCAPE。
     */
    @Select("SELECT MAX(tag_no) FROM t_ear_tag WHERE tag_no LIKE CONCAT(#{prefix}, '%') ESCAPE '\\\\'")
    String selectMaxTagNo(@Param("prefix") String prefix);
}
