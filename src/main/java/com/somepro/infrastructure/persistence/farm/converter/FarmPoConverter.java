package com.somepro.infrastructure.persistence.farm.converter;

import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.model.FarmStatus;
import com.somepro.domain.shared.model.Species;
import com.somepro.infrastructure.persistence.farm.po.FarmPO;

/**
 * FarmPO（表）↔ Farm（领域）转换器（基础设施层），PO 与领域之间唯一的转换入口。
 *
 * 枚举以名称字符串落库；库里若出现代码未覆盖的枚举值（历史数据/手工改库），
 * 直接透传成 null 字段会丢失原值，因此这里对非法值保持原样失败（IllegalArgumentException）——
 * 存量数据必须能翻出来是硬要求，若真有脏值应先修数据而不是静默吞掉。
 */
public final class FarmPoConverter {

    private FarmPoConverter() {
    }

    public static FarmPO toPo(Farm domain) {
        FarmPO po = new FarmPO();
        po.setId(domain.getId());
        po.setFarmNo(domain.getFarmNo());
        po.setFarmName(domain.getFarmName());
        po.setOwnerName(domain.getOwnerName());
        po.setPhone(domain.getPhone());
        po.setAddress(domain.getAddress());
        po.setSpecies(domain.getSpecies() == null ? null : domain.getSpecies().name());
        po.setStockQty(domain.getStockQty());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static Farm toDomain(FarmPO po) {
        Farm domain = new Farm();
        domain.setId(po.getId());
        domain.setFarmNo(po.getFarmNo());
        domain.setFarmName(po.getFarmName());
        domain.setOwnerName(po.getOwnerName());
        domain.setPhone(po.getPhone());
        domain.setAddress(po.getAddress());
        domain.setSpecies(Species.valueOf(po.getSpecies()));
        domain.setStockQty(po.getStockQty());
        domain.setStatus(FarmStatus.valueOf(po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
