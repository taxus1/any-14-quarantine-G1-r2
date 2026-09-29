package com.somepro.domain.eartag.model;

import com.somepro.common.exception.BizException;

/**
 * 耳标业务状态。新领耳标默认 {@link #ISSUED}。
 */
public enum EarTagStatus {

    /** 已发放待佩戴 */
    ISSUED,
    /** 已佩戴 */
    USED,
    /** 遗失 */
    LOST,
    /** 停用 */
    DISABLED;

    public static EarTagStatus of(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return EarTagStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("耳标状态不合法，仅支持 ISSUED / USED / LOST / DISABLED");
        }
    }
}
