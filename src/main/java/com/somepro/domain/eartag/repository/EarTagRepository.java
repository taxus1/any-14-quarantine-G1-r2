package com.somepro.domain.eartag.repository;

import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import com.somepro.domain.eartag.model.EarTagStatus;
import reactor.core.publisher.Mono;

/**
 * 耳标仓储端口（领域层定义，基础设施层实现）。
 *
 * 按场 / 种类 / 状态筛选翻页，条件任意组合，全部为空时返回全量。
 */
public interface EarTagRepository {

    /**
     * 保存：id 为空走插入并分配耳标号（ET-年份-6 位顺序号，年份内顺序递增、不重号），
     * id 非空走更新。
     */
    Mono<EarTag> save(EarTag earTag);

    Mono<EarTag> findById(Long id);

    Mono<PageResult<EarTag>> page(int pageNum, int pageSize,
                                  Long farmId, Species species, EarTagStatus status, String tagNo);

    /** 软删除（del_flag 置 1），返回是否命中未删除的记录。 */
    Mono<Boolean> softDelete(Long id);
}
