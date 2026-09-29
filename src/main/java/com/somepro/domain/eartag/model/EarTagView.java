package com.somepro.domain.eartag.model;

import com.somepro.domain.shared.model.Species;

import java.time.LocalDateTime;

/**
 * 耳标名册读模型（领域值对象）：在耳标自身字段之外补一个所属场编号 farmNo。
 *
 * 列表接口要求「每行把耳标号带出来」并标明挂在哪家场，而耳标表本身只存 farm_id，
 * 仓储在分页查询时批量回填场编号，组装成本读模型。它只承载读数据、无行为，用 record。
 *
 * @param farmNo 所属养殖场编号（查不到场档案时为 null，说明数据被物理清理过）
 */
public record EarTagView(Long id, String tagNo, Long farmId, String farmNo, Species species,
                         LocalDateTime issuedAt, LocalDateTime wornAt, EarTagStatus status,
                         LocalDateTime createTime) {
}
