package com.somepro.domain.shared.model;

import com.somepro.common.exception.BizException;

/**
 * 畜禽种类（领域共享枚举，纯领域、无框架注解）。
 *
 * 养殖场档案与耳标共用同一套种类代码；耳标不单独选种类，
 * 领标/挂标时直接照所属养殖场的种类走。
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

    /** 按代码解析；库里/入参出现不认识的种类属于业务错误，明确报错而不是吞成 null。 */
    public static Species fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("养殖种类不能为空");
        }
        try {
            return Species.valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            throw new BizException("不支持的养殖种类：" + code);
        }
    }
}
