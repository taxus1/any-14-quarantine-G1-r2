package com.somepro.application.farm;

import com.somepro.common.exception.BizException;
import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.model.FarmStatus;
import com.somepro.domain.farm.repository.FarmRepository;
import com.somepro.domain.shared.model.Change;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 养殖场档案应用服务：用例编排（登记、修改、查名册、注销），不写业务规则（规则在 {@link Farm}）。
 *
 * 出入参只认领域对象与基础类型，不认识 PO / VO。
 */
@Service
public class FarmAppService {

    private final FarmRepository farmRepository;

    public FarmAppService(FarmRepository farmRepository) {
        this.farmRepository = farmRepository;
    }

    /** 登记新场：编号与雪花 ID 由仓储分配，状态默认在用。 */
    public Mono<Farm> register(String farmName, String ownerName, String phone, String address,
                               String species, Integer stockQty) {
        Farm farm = Farm.register(farmName, ownerName, phone, address, Species.of(species), stockQty);
        return farmRepository.create(farm);
    }

    /**
     * 改档案：每个字段用 {@link Change} 区分三态（没传保持 / 空串清空 / 正常值改）。
     * 场编号不可改。枚举重解析在进入领域前完成，非法值在这里就转业务异常。
     */
    public Mono<Farm> edit(Long id,
                           Change<String> farmName, Change<String> ownerName,
                           Change<String> phone, Change<String> address,
                           Change<String> species, Change<Integer> stockQty,
                           Change<String> status) {
        // species/status 是非空枚举：传空串不算清空（列不允许 null），按「不改」处理；非法值由 of 抛异常。
        Change<Species> speciesChange = blankToAbsent(species.map(Species::of));
        Change<FarmStatus> statusChange = blankToAbsent(status.map(FarmStatus::of));
        // 存栏是非空整数：传空串同样按「不改」，不要把 NOT NULL 列清成 null
        Change<Integer> stockChange = stockQty.present() && stockQty.value() == null
                ? Change.<Integer>absent() : stockQty;
        return requireFarm(id).flatMap(farm -> {
            farm.edit(farmName, ownerName, phone, address, speciesChange, stockChange, statusChange);
            return farmRepository.update(farm);
        });
    }

    private <T> Change<T> blankToAbsent(Change<T> change) {
        return change.present() && change.value() == null ? Change.absent() : change;
    }

    public Mono<Farm> detail(Long id) {
        return requireFarm(id);
    }

    /**
     * 翻名册：场名、编号、种类、状态四个条件任意组合，全不填返回整本名册。
     */
    public Mono<PageResult<Farm>> page(int pageNum, int pageSize,
                                       String farmName, String farmNo, String species, String status) {
        return farmRepository.page(pageNum, pageSize, farmName, farmNo,
                Species.of(species), FarmStatus.of(status));
    }

    /** 注销：销掉后不再出现在任何名册（软删除）。 */
    public Mono<Void> close(Long id) {
        return requireFarm(id).then(farmRepository.softDelete(id));
    }

    private Mono<Farm> requireFarm(Long id) {
        return farmRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("养殖场不存在或已注销")));
    }
}
