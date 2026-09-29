package com.somepro.interfaces.rest.farm.converter;

import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.farm.vo.FarmVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Farm（领域）→ FarmVO（对外）转换器（接口层）。
 */
public final class FarmVoConverter {

    private FarmVoConverter() {
    }

    public static FarmVO toVo(Farm domain) {
        return new FarmVO(
                domain.getId(),
                domain.getFarmNo(),
                domain.getFarmName(),
                domain.getOwnerName(),
                domain.getPhone(),
                domain.getAddress(),
                domain.getSpecies() == null ? null : domain.getSpecies().name(),
                domain.getStockQty(),
                domain.getStatus() == null ? null : domain.getStatus().name(),
                domain.getCreateTime());
    }

    public static PageVO<FarmVO> toPageVo(PageResult<Farm> page) {
        List<FarmVO> content = page.content().stream()
                .map(FarmVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
