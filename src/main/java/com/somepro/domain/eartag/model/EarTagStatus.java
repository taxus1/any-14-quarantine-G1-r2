package com.somepro.domain.eartag.model;

import com.somepro.common.exception.BizException;

/**
 * 耳标状态（领域枚举）。
 *
 * ISSUED 已发放待佩戴（刚领默认）/ USED 已佩戴 / LOST 遗失 / DISABLED 停用。
 */
public enum EarTagStatus {

    ISSUED,
    USED,
    LOST,
    DISABLED;

    public static EarTagStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("耳标状态不能为空");
        }
        try {
            return EarTagStatus.valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            throw new BizException("不支持的耳标状态：" + code);
        }
    }
}
