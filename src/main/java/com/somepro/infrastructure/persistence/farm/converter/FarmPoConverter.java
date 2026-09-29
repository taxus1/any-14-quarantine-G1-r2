package com.somepro.infrastructure.persistence.farm.converter;

import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.model.FarmStatus;
import com.somepro.domain.shared.model.Species;
import com.somepro.infrastructure.persistence.farm.po.FarmPO;

/**
 * FarmPO（表）↔ Farm（领域）转换器（基础设施层）。
 *
 * PO 只存枚举代码字符串，种类/状态的合法性由领域枚举 fromCode 把关；
 * 审计字段与 delFlag 一并搬运。
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
        // 库里的历史数据同样按代码解析；不认识的代码明确报错，不当作没有
        domain.setSpecies(po.getSpecies() == null ? null : Species.fromCode(po.getSpecies()));
        domain.setStockQty(po.getStockQty());
        domain.setStatus(po.getStatus() == null ? null : FarmStatus.fromCode(po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
