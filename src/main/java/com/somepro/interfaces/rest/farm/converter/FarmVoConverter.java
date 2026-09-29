package com.somepro.interfaces.rest.farm.converter;

import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.farm.vo.FarmVO;
import com.somepro.interfaces.rest.shared.PageVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Farm（领域）→ FarmVO（对外）转换器（用户接口层）。
 * Controller 不许直接把领域对象塞进 Result 返回，内部字段只在这里做白名单裁剪。
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
