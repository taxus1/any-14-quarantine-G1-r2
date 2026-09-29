package com.somepro.application.farm.command;

/**
 * 登记养殖场命令（应用层入参，不可变 record）。
 *
 * 场编号与状态不在命令里：编号由仓储生成，新场默认 ACTIVE。
 */
public record FarmCreateCommand(String farmName,
                                String ownerName,
                                String phone,
                                String address,
                                String species,
                                Integer stockQty) {
}
