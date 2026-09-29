package com.somepro.interfaces.rest.farm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 养殖场状态变更请求体（接口层，不可变 record）。
 */
public record FarmStatusRequest(
        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "ACTIVE|SUSPENDED|CLOSED",
                message = "养殖场状态只能是 ACTIVE/SUSPENDED/CLOSED")
        String status) {
}
