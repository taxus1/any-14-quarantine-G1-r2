package com.somepro.interfaces.rest.eartag.converter;

import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagView;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.eartag.vo.EarTagVO;
import com.somepro.interfaces.rest.shared.PageVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 耳标领域对象 → 对外 VO 转换器（用户接口层）。
 * 列表读模型 EarTagView 带 farmNo，明细 EarTag 不带（明细只回场 id，需要场编号可查场档案）。
 */
public final class EarTagVoConverter {

    private EarTagVoConverter() {
    }

    public static EarTagVO toVo(EarTag domain) {
        return new EarTagVO(
                domain.getId(),
                domain.getTagNo(),
                domain.getFarmId(),
                null,
                domain.getSpecies() == null ? null : domain.getSpecies().name(),
                domain.getIssuedAt(),
                domain.getWornAt(),
                domain.getStatus() == null ? null : domain.getStatus().name(),
                domain.getCreateTime());
    }

    public static EarTagVO toVo(EarTagView view) {
        return new EarTagVO(
                view.id(),
                view.tagNo(),
                view.farmId(),
                view.farmNo(),
                view.species() == null ? null : view.species().name(),
                view.issuedAt(),
                view.wornAt(),
                view.status() == null ? null : view.status().name(),
                view.createTime());
    }

    public static PageVO<EarTagVO> toPageVo(PageResult<EarTagView> page) {
        List<EarTagVO> content = page.content().stream()
                .map(EarTagVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
