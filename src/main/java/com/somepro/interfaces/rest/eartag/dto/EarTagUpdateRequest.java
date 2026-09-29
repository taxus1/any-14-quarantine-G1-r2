package com.somepro.interfaces.rest.eartag.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

/**
 * 修改耳标请求体（接口层，不可变 record）。
 *
 * 字段缺省（null）表示不改。
 * - 改 farmId 即改挂场，种类自动同步成新场种类；
 * - status 传 USED 时自动登记佩戴时刻（wornAt 没传则取当前时间）。
 */
public record EarTagUpdateRequest(
        Long farmId,

        @Pattern(regexp = "ISSUED|USED|LOST|DISABLED",
                message = "耳标状态只能是 ISSUED/USED/LOST/DISABLED")
        String status,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime issuedAt,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime wornAt) {
}
