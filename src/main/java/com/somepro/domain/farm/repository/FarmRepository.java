package com.somepro.domain.farm.repository;

import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.model.FarmStatus;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import reactor.core.publisher.Mono;

/**
 * 养殖场档案仓储端口（领域层定义，基础设施层实现）。
 */
public interface FarmRepository {

    /** 新建落库：仓储负责分配场编号、雪花 ID，并处理编号唯一冲突（重试换号）。 */
    Mono<Farm> create(Farm farm);

    /** 修改落库（按 id 更新，只改未删除的行）。 */
    Mono<Farm> update(Farm farm);

    Mono<Farm> findById(Long id);

    /**
     * 分页名册。任意条件可空：
     * @param farmName 场名模糊匹配
     * @param farmNo   场编号模糊匹配
     * @param species  养殖种类精确匹配
     * @param status   场状态精确匹配
     * 自动排除已注销（软删）的档案；按 id 升序，保证翻页不重不漏。
     */
    Mono<PageResult<Farm>> page(int pageNum, int pageSize,
                                String farmName, String farmNo, Species species, FarmStatus status);

    /** 注销（软删除）。 */
    Mono<Void> softDelete(Long id);
}
