package com.somepro.infrastructure.persistence.eartag.converter;

import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagStatus;
import com.somepro.domain.shared.model.Species;
import com.somepro.infrastructure.persistence.eartag.po.EarTagPO;

/**
 * EarTagPO（表）↔ EarTag（领域）转换器（基础设施层）。
 *
 * PO 只存枚举代码字符串，种类/状态的合法性由领域枚举 fromCode 把关；
 * 审计字段与 delFlag 一并搬运。
 */
public final class EarTagPoConverter {

    private EarTagPoConverter() {
    }

    public static EarTagPO toPo(EarTag domain) {
        EarTagPO po = new EarTagPO();
        po.setId(domain.getId());
        po.setTagNo(domain.getTagNo());
        po.setFarmId(domain.getFarmId());
        po.setSpecies(domain.getSpecies() == null ? null : domain.getSpecies().name());
        po.setIssuedAt(domain.getIssuedAt());
        po.setWornAt(domain.getWornAt());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static EarTag toDomain(EarTagPO po) {
        EarTag domain = new EarTag();
        domain.setId(po.getId());
        domain.setTagNo(po.getTagNo());
        domain.setFarmId(po.getFarmId());
        domain.setSpecies(po.getSpecies() == null ? null : Species.fromCode(po.getSpecies()));
        domain.setIssuedAt(po.getIssuedAt());
        domain.setWornAt(po.getWornAt());
        domain.setStatus(po.getStatus() == null ? null : EarTagStatus.fromCode(po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
