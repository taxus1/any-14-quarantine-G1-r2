package com.somepro.interfaces.rest.eartag.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 耳标对外返回对象（接口层，不可变 record）。
 *
 * 每行都带 tagNo，方便与纸质台账对号。
 * 刻意不含 delFlag / createBy / updateBy 等内部字段。
 */
public record EarTagVO(Long id,
                       String tagNo,
                       Long farmId,
                       String species,
                       LocalDateTime issuedAt,
                       LocalDateTime wornAt,
                       String status,
                       LocalDateTime createTime) implements Serializable {
}
