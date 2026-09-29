package com.somepro.interfaces.rest.farm.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 养殖场档案对外对象（VO，不可变 record）。
 *
 * 每行必带场编号 farmNo，方便与纸质台账对号。
 * 枚举以字符串名输出（PIG / ACTIVE 等）；刻意不含 delFlag 与审计人等内部字段。
 */
public record FarmVO(Long id, String farmNo, String farmName, String ownerName, String phone,
                     String address, String species, Integer stockQty, String status,
                     LocalDateTime createTime) implements Serializable {
}
