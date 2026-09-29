package com.somepro.interfaces.rest.eartag.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 发放耳标请求体（接口层，不可变 record）。
 *
 * 领标不选种类：只填挂到哪家场，种类照该场走。
 */
public record EarTagIssueRequest(
        @NotNull(message = "养殖场 id 不能为空")
        Long farmId,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime issuedAt) {
}
