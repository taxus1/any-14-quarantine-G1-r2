package com.somepro.interfaces.rest.farm.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 养殖场对外返回对象（接口层，不可变 record）。
 *
 * 每行都带 farmNo，方便与纸质台账对号。
 * 刻意不含 delFlag / createBy / updateBy 等内部字段。
 */
public record FarmVO(Long id,
                     String farmNo,
                     String farmName,
                     String ownerName,
                     String phone,
                     String address,
                     String species,
                     Integer stockQty,
                     String status,
                     LocalDateTime createTime) implements Serializable {
}
