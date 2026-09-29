package com.somepro.interfaces.rest.farm.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 修改养殖场档案请求体（接口层，不可变 record）。
 *
 * 所有字段可选：字段缺省（null）表示该项不改。场编号不可改。
 */
public record FarmUpdateRequest(
        @Size(max = 64, message = "养殖场名称最长 64 个字符")
        String farmName,

        @Size(max = 64, message = "负责人最长 64 个字符")
        String ownerName,

        @Size(max = 20, message = "联系方式最长 20 个字符")
        String phone,

        @Size(max = 255, message = "场址最长 255 个字符")
        String address,

        @Pattern(regexp = "PIG|CATTLE|SHEEP|POULTRY", message = "养殖种类只能是 PIG/CATTLE/SHEEP/POULTRY")
        String species,

        @PositiveOrZero(message = "存栏数不能为负数")
        Integer stockQty) {
}
