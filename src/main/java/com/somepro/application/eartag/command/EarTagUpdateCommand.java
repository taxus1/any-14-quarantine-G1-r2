package com.somepro.application.eartag.command;

import java.time.LocalDateTime;

/**
 * 修改耳标命令（应用层入参，不可变 record）。
 *
 * 字段为 null 表示该项不改。
 * - farmId 改挂场时，种类自动同步成新场的种类（应用层查场后带入）；
 * - 状态改成 USED 时记佩戴时刻；issuedAt/wornAt 也允许直接登记。
 */
public record EarTagUpdateCommand(Long farmId,
                                  String status,
                                  LocalDateTime issuedAt,
                                  LocalDateTime wornAt) {
}
