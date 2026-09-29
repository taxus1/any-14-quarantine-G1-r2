package com.somepro.interfaces.rest.eartag.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 畜禽耳标对外对象（VO，不可变 record）。
 *
 * 每行必带耳标号 tagNo 与所属场编号 farmNo（由仓储批量回填），方便与养殖场台账对号。
 * species 是挂领时从场档案带出的，不是领用时填的；刻意不含内部软删/审计字段。
 */
public record EarTagVO(Long id, String tagNo, Long farmId, String farmNo, String species,
                       LocalDateTime issuedAt, LocalDateTime wornAt, String status,
                       LocalDateTime createTime) implements Serializable {
}
