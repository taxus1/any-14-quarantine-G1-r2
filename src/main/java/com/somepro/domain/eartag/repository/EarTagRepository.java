package com.somepro.domain.eartag.repository;

import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagStatus;
import com.somepro.domain.eartag.model.EarTagView;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import reactor.core.publisher.Mono;

/**
 * 耳标仓储端口（领域层定义，基础设施层实现）。
 */
public interface EarTagRepository {

    /** 新建落库：仓储负责分配耳标号、雪花 ID，并处理编号唯一冲突（重试换号）。 */
    Mono<EarTag> create(EarTag earTag);

    /** 修改落库（按 id 更新，只改未删除的行）。 */
    Mono<EarTag> update(EarTag earTag);

    Mono<EarTag> findById(Long id);

    /**
     * 分页名册。任意条件可空：
     * @param farmId  所属场精确匹配
     * @param species 种类精确匹配
     * @param status  状态精确匹配
     * 结果每行补上场编号（farmNo），方便与养殖场台账对号；
     * 自动排除已注销（软删）的耳标，按 id 升序翻页。
     */
    Mono<PageResult<EarTagView>> page(int pageNum, int pageSize,
                                      Long farmId, Species species, EarTagStatus status);

    /** 注销（软删除）。 */
    Mono<Void> softDelete(Long id);
}
