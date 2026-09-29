package com.somepro.application.eartag.command;

import java.time.LocalDateTime;

/**
 * 发放耳标命令（应用层入参，不可变 record）。
 *
 * 领标不选种类：种类由所属养殖场带出，故命令里只有 farmId。
 */
public record EarTagIssueCommand(Long farmId, LocalDateTime issuedAt) {
}
