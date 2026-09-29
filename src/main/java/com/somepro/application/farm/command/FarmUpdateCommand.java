package com.somepro.application.farm.command;

/**
 * 修改养殖场命令（应用层入参，不可变 record）。
 *
 * 字段为 null 表示该项不改；场编号不可改，故命令里没有 farmNo。
 * status 单独走状态变更用例（注销/停业/恢复）。
 */
public record FarmUpdateCommand(String farmName,
                                String ownerName,
                                String phone,
                                String address,
                                String species,
                                Integer stockQty) {
}
