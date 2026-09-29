package com.somepro.infrastructure.persistence.eartag;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.eartag.po.EarTagPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 畜禽耳标 Mapper（基础设施层，阻塞 JDBC，只能在 boundedElastic 线程调用）。
 */
@Mapper
public interface EarTagMapper extends BaseMapper<EarTagPO> {

    /**
     * 取某编号前缀下的全部耳标号（含已软删记录），供应用层算出年内最大顺序号。
     * 与 {@code FarmMapper#selectAllNosLike} 同理，刻意不加 del_flag 过滤。
     */
    @Select("SELECT tag_no FROM t_ear_tag WHERE tag_no LIKE #{prefix} ESCAPE '\\\\'")
    List<String> selectAllNosLike(@Param("prefix") String prefix);
}
