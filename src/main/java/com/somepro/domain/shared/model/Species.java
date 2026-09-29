package com.somepro.domain.shared.model;

import com.somepro.common.exception.BizException;

/**
 * 畜禽种类（领域共享枚举）：养殖场与耳标共用同一套取值。
 *
 * 耳标的种类不允许单独指定，必须与所属养殖场一致（领标时从场档案带出），
 * 因此放在共享层供两个限界上下文引用。
 */
public enum Species {

    /** 生猪 */
    PIG,
    /** 牛 */
    CATTLE,
    /** 羊 */
    SHEEP,
    /** 禽 */
    POULTRY;

    /** 容错解析：空白按未传（null）处理；非法值抛业务异常，由全局异常收口成统一 Result。 */
    public static Species of(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Species.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("养殖种类不合法，仅支持 PIG / CATTLE / SHEEP / POULTRY");
        }
    }
}
