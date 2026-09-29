package com.somepro.interfaces.rest.farm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 登记养殖场请求体（接口层，不可变 record）。
 *
 * 场编号与状态不收：编号系统生成，新场默认 ACTIVE。
 * 枚举用白名单正则校验，非法值在进应用层之前就被拦下。
 */
public record FarmCreateRequest(
        @NotBlank(message = "养殖场名称不能为空")
        @Size(max = 64, message = "养殖场名称最长 64 个字符")
        String farmName,

        @Size(max = 64, message = "负责人最长 64 个字符")
        String ownerName,

        @Size(max = 20, message = "联系方式最长 20 个字符")
        String phone,

        @Size(max = 255, message = "场址最长 255 个字符")
        String address,

        @NotNull(message = "养殖种类不能为空（PIG/CATTLE/SHEEP/POULTRY）")
        @Pattern(regexp = "PIG|CATTLE|SHEEP|POULTRY", message = "养殖种类只能是 PIG/CATTLE/SHEEP/POULTRY")
        String species,

        @PositiveOrZero(message = "存栏数不能为负数")
        Integer stockQty) {
}
