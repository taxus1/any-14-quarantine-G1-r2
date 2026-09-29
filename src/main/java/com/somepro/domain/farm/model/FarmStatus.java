package com.somepro.domain.farm.model;

import com.somepro.common.exception.BizException;

/**
 * 养殖场业务状态。
 *
 * 新立养殖场默认 {@link #ACTIVE}；「注销」是档案级销毁动作，走软删除
 * （@TableLogic，注销后不再出现在任何名单里），而不是把状态改成 CLOSED ——
 * CLOSED 作为状态值保留（登记/修改接口允许手工把状态置成它，表示业务上已停业注销但档案留存），
 * 真正的「销档」只有软删除一条路。
 */
public enum FarmStatus {

    /** 在用 */
    ACTIVE,
    /** 停业 */
    SUSPENDED,
    /** 注销（状态值；档案注销动作本身走软删除） */
    CLOSED;

    /** 容错解析：空白按未传（null）处理；非法值抛业务异常。 */
    public static FarmStatus of(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return FarmStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("场状态不合法，仅支持 ACTIVE / SUSPENDED / CLOSED");
        }
    }
}
