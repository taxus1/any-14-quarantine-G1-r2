package com.somepro.domain.farm.model;

import com.somepro.common.exception.BizException;

/**
 * 养殖场状态（领域枚举）。
 *
 * ACTIVE 在用（新立场默认）/ SUSPENDED 停业 / CLOSED 注销。
 * 注意「注销」是业务状态，仍留在名册里可按状态筛出来；
 * 真正「不再从名单里翻出来」走软删除（del_flag=1）。
 */
public enum FarmStatus {

    ACTIVE,
    SUSPENDED,
    CLOSED;

    public static FarmStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("养殖场状态不能为空");
        }
        try {
            return FarmStatus.valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            throw new BizException("不支持的养殖场状态：" + code);
        }
    }
}
