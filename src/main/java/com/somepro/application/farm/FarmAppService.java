package com.somepro.application.farm;

import com.somepro.application.farm.command.FarmCreateCommand;
import com.somepro.application.farm.command.FarmUpdateCommand;
import com.somepro.common.exception.BizException;
import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.model.FarmStatus;
import com.somepro.domain.farm.repository.FarmRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 养殖场档案应用服务：编排登记、改档、注销/停业、查询、销档用例。
 *
 * 不写业务规则（在 Farm 聚合里），出入参是领域对象与本层 command，不认识 PO/VO。
 */
@Service
public class FarmAppService {

    private final FarmRepository farmRepository;

    public FarmAppService(FarmRepository farmRepository) {
        this.farmRepository = farmRepository;
    }

    /** 登记新场：编号由仓储分配，状态默认 ACTIVE。 */
    public Mono<Farm> create(FarmCreateCommand cmd) {
        Farm farm = Farm.register(cmd.farmName(), cmd.ownerName(), cmd.phone(), cmd.address(),
                Species.fromCode(cmd.species()), cmd.stockQty());
        return farmRepository.save(farm);
    }

    public Mono<Farm> detail(Long id) {
        return requireFarm(id);
    }

    /** 改档案（场名、负责人、联系方式、场址、种类、存栏）。 */
    public Mono<Farm> modify(Long id, FarmUpdateCommand cmd) {
        return requireFarm(id).flatMap(farm -> {
            farm.changeProfile(cmd.farmName(), cmd.ownerName(), cmd.phone(), cmd.address(),
                    cmd.species() == null ? null : Species.fromCode(cmd.species()), cmd.stockQty());
            return farmRepository.save(farm);
        });
    }

    /** 变更业务状态：ACTIVE/SUSPENDED/CLOSED（注销走这里，注销后仍在名册、可按状态查到）。 */
    public Mono<Farm> changeStatus(Long id, String statusCode) {
        FarmStatus target = FarmStatus.fromCode(statusCode);
        return requireFarm(id).flatMap(farm -> {
            farm.changeStatus(target);
            return farmRepository.save(farm);
        });
    }

    /** 销档：软删除，之后名单里翻不出来。 */
    public Mono<Void> delete(Long id) {
        return farmRepository.softDelete(id).flatMap(hit -> {
            if (!hit) {
                return Mono.error(new BizException("养殖场不存在或已注销：id=" + id));
            }
            return Mono.empty();
        });
    }

    /** 名册：场名、编号、种类、状态任意组合，都不填返回全名册。 */
    public Mono<PageResult<Farm>> page(int pageNum, int pageSize,
                                       String farmName, String farmNo, String species, String status) {
        Species speciesFilter = (species == null || species.isBlank()) ? null : Species.fromCode(species);
        FarmStatus statusFilter = (status == null || status.isBlank()) ? null : FarmStatus.fromCode(status);
        return farmRepository.page(pageNum, pageSize, farmName, farmNo, speciesFilter, statusFilter);
    }

    private Mono<Farm> requireFarm(Long id) {
        return farmRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("养殖场不存在：id=" + id)));
    }
}
