package com.somepro.domain.farm.repository;

import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import com.somepro.domain.farm.model.FarmStatus;
import reactor.core.publisher.Mono;

/**
 * 养殖场仓储端口（领域层定义，基础设施层实现）。
 *
 * 四个筛选条件任意组合，全部为 null/空时返回整份名册；分页返回领域 PageResult。
 */
public interface FarmRepository {

    /**
     * 保存：id 为空走插入并分配场编号（FM-年份-4 位顺序号，年份内顺序递增、不重号），
     * id 非空走更新。
     */
    Mono<Farm> save(Farm farm);

    Mono<Farm> findById(Long id);

    Mono<PageResult<Farm>> page(int pageNum, int pageSize,
                                String farmName, String farmNo, Species species, FarmStatus status);

    /** 软删除（del_flag 置 1），返回是否命中未删除的记录。 */
    Mono<Boolean> softDelete(Long id);
}
