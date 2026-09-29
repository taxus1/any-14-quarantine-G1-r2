package com.somepro.application.eartag;

import com.somepro.common.exception.BizException;
import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagStatus;
import com.somepro.domain.eartag.model.EarTagView;
import com.somepro.domain.eartag.repository.EarTagRepository;
import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.repository.FarmRepository;
import com.somepro.domain.shared.model.Change;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * 畜禽耳标应用服务：发放（领用）、修改、翻名册、注销。
 *
 * 关键用例规则：领耳标时不收种类 —— 只收养殖场 id，种类从该场档案带出，
 * 保证场里场外对不上的情况不可能发生；改挂养殖场同样由新场带出种类。
 */
@Service
public class EarTagAppService {

    private final EarTagRepository earTagRepository;
    private final FarmRepository farmRepository;

    public EarTagAppService(EarTagRepository earTagRepository, FarmRepository farmRepository) {
        this.earTagRepository = earTagRepository;
        this.farmRepository = farmRepository;
    }

    /**
     * 发放（领用）耳标。
     *
     * @param farmId   挂领的养殖场 id（必填）
     * @param issuedAt 发放时刻，可空（默认领用当下）
     */
    public Mono<EarTag> issue(Long farmId, LocalDateTime issuedAt) {
        return requireFarm(farmId).flatMap(farm -> {
            EarTag tag = EarTag.issue(farm.getId(), farm.getSpecies(), issuedAt);
            return earTagRepository.create(tag);
        });
    }

    /**
     * 修改耳标（部分更新）。
     * 传 farmId（改挂场）时种类由新场档案带出；时间字段传空串清空；状态由非佩戴变 USED 且
     * 未给 wornAt 时，领域自动把佩戴时刻记为当下。
     */
    public Mono<EarTag> edit(Long id, Change<Long> farmId, Change<LocalDateTime> issuedAt,
                             Change<LocalDateTime> wornAt, Change<String> status) {
        // 状态是非空枚举：传空串不表示清空（库里不允许 null），按「不改」处理；传非法值由 of 抛业务异常。
        Change<EarTagStatus> parsed = status.map(EarTagStatus::of);
        Change<EarTagStatus> statusChange = parsed.present() && parsed.value() == null
                ? Change.absent() : parsed;
        return requireTag(id).flatMap(tag ->
                resolveSpecies(farmId).flatMap(speciesChange -> {
                    tag.edit(farmId, speciesChange, issuedAt, wornAt, statusChange);
                    return earTagRepository.update(tag);
                }));
    }

    public Mono<EarTag> detail(Long id) {
        return requireTag(id);
    }

    /**
     * 翻名册：按场、种类、状态任意组合过滤，全不填返回全部；每行带耳标号与所属场编号。
     */
    public Mono<PageResult<EarTagView>> page(int pageNum, int pageSize,
                                             Long farmId, String species, String status) {
        return earTagRepository.page(pageNum, pageSize, farmId,
                Species.of(species), EarTagStatus.of(status));
    }

    /** 注销耳标（软删除）。 */
    public Mono<Void> disable(Long id) {
        return requireTag(id).then(earTagRepository.softDelete(id));
    }

    private Mono<EarTag> requireTag(Long id) {
        return earTagRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("耳标不存在或已注销")));
    }

    private Mono<Farm> requireFarm(Long farmId) {
        return farmRepository.findById(farmId)
                .switchIfEmpty(Mono.error(new BizException("养殖场不存在或已注销，耳标无法登记")));
    }

    /**
     * 解析本次修改要随场带出的种类：
     * - farmId 缺键（absent，不改挂场）→ 返回 absent，领域不改场也不改种类；
     * - farmId 有值（含 set 空值，会在领域 attachTo 被拦）→ 先查场，再用 set 装新场种类。
     */
    private Mono<Change<Species>> resolveSpecies(Change<Long> farmId) {
        if (!farmId.present()) {
            return Mono.just(Change.absent());
        }
        return requireFarm(farmId.value()).map(farm -> Change.set(farm.getSpecies()));
    }
}
