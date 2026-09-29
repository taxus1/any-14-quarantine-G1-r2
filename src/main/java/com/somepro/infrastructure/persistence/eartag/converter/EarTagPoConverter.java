package com.somepro.infrastructure.persistence.eartag.converter;

import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagStatus;
import com.somepro.domain.eartag.model.EarTagView;
import com.somepro.domain.shared.model.Species;
import com.somepro.infrastructure.persistence.eartag.po.EarTagPO;

/**
 * EarTagPO（表）↔ EarTag（领域）转换器（基础设施层）。
 * {@link #toView} 额外把分页查询回填的场编号组装进耳标名册读模型。
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
        domain.setSpecies(Species.valueOf(po.getSpecies()));
        domain.setIssuedAt(po.getIssuedAt());
        domain.setWornAt(po.getWornAt());
        domain.setStatus(EarTagStatus.valueOf(po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }

    /** PO + 所属场编号 → 名册读模型。farmNo 由仓储批量查 t_farm 回填。 */
    public static EarTagView toView(EarTagPO po, String farmNo) {
        return new EarTagView(
                po.getId(),
                po.getTagNo(),
                po.getFarmId(),
                farmNo,
                Species.valueOf(po.getSpecies()),
                po.getIssuedAt(),
                po.getWornAt(),
                EarTagStatus.valueOf(po.getStatus()),
                po.getCreateTime());
    }
}
