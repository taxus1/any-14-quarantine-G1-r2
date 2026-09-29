package com.somepro.application.eartag;

import com.somepro.application.eartag.command.EarTagIssueCommand;
import com.somepro.application.eartag.command.EarTagUpdateCommand;
import com.somepro.common.exception.BizException;
import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagStatus;
import com.somepro.domain.eartag.repository.EarTagRepository;
import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.repository.FarmRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 耳标应用服务：编排发放、改挂/改态、查询、缴销用例。
 *
 * 耳标不能脱离养殖场选种类，所以凡是涉及归属场的用例都在这里先查场、
 * 再把场的种类交给 EarTag 聚合 —— 跨聚合引用以 id 相连，种类快照由本层带出。
 */
@Service
public class EarTagAppService {

    private final EarTagRepository earTagRepository;
    private final FarmRepository farmRepository;

    public EarTagAppService(EarTagRepository earTagRepository, FarmRepository farmRepository) {
        this.earTagRepository = earTagRepository;
        this.farmRepository = farmRepository;
    }

    /** 领标：只给场，种类照场走；默认 ISSUED，发放时刻缺省取当前时间。 */
    public Mono<EarTag> issue(EarTagIssueCommand cmd) {
        return requireFarm(cmd.farmId()).flatMap(farm -> {
            EarTag tag = EarTag.issue(farm.getId(), farm.getSpecies(), cmd.issuedAt());
            return earTagRepository.save(tag);
        });
    }

    public Mono<EarTag> detail(Long id) {
        return requireTag(id);
    }

    /** 改耳标：改挂场则种类跟随新场；状态改 USED 则登记佩戴时刻。 */
    public Mono<EarTag> modify(Long id, EarTagUpdateCommand cmd) {
        return requireTag(id).flatMap(tag ->
                resolveTargetFarm(cmd.farmId()).flatMap(targetFarm -> {
                    if (targetFarm != null) {
                        // 场里怎么定的种类，耳标就照什么样，避免场里场外对不上
                        tag.attachTo(targetFarm.getId(), targetFarm.getSpecies());
                    }
                    if (cmd.status() != null) {
                        EarTagStatus target = EarTagStatus.fromCode(cmd.status());
                        if (target == EarTagStatus.USED) {
                            tag.markWorn(cmd.wornAt());
                        } else {
                            tag.changeStatus(target);
                        }
                    }
                    if (cmd.issuedAt() != null) {
                        tag.setIssuedAt(cmd.issuedAt());
                    }
                    if (cmd.wornAt() != null) {
                        tag.setWornAt(cmd.wornAt());
                    }
                    return earTagRepository.save(tag);
                })
        );
    }

    /** 缴销耳标：软删除。 */
    public Mono<Void> delete(Long id) {
        return earTagRepository.softDelete(id).flatMap(hit -> {
            if (!hit) {
                return Mono.error(new BizException("耳标不存在或已缴销：id=" + id));
            }
            return Mono.empty();
        });
    }

    /** 耳标名册：按场、种类、状态翻，耳标号也支持精确片段查询；都不填返回全量。 */
    public Mono<PageResult<EarTag>> page(int pageNum, int pageSize,
                                         Long farmId, String species, String status, String tagNo) {
        Species speciesFilter = (species == null || species.isBlank()) ? null : Species.fromCode(species);
        EarTagStatus statusFilter = (status == null || status.isBlank()) ? null : EarTagStatus.fromCode(status);
        return earTagRepository.page(pageNum, pageSize, farmId, speciesFilter, statusFilter, tagNo);
    }

    private Mono<Farm> requireFarm(Long farmId) {
        if (farmId == null) {
            return Mono.error(new BizException("养殖场 id 不能为空"));
        }
        return farmRepository.findById(farmId)
                .switchIfEmpty(Mono.error(new BizException("养殖场不存在：id=" + farmId)));
    }

    /** farmId 为 null 表示不改归属；否则必须能查到（已软删的场同样查不到）。 */
    private Mono<Farm> resolveTargetFarm(Long farmId) {
        if (farmId == null) {
            return Mono.just(null);
        }
        return requireFarm(farmId);
    }

    private Mono<EarTag> requireTag(Long id) {
        return earTagRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("耳标不存在：id=" + id)));
    }
}
