package com.somepro.infrastructure.persistence.farm.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * t_farm 表的持久化对象（PO，基础设施层）：字段与列一一对应，不放业务规则。
 * 枚举（species / status）以列里的字符串名原样存取。
 */
@Getter
@Setter
@TableName("t_farm")
public class FarmPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 养殖场编号，全局唯一，如 FM-2026-0001。 */
    @TableField("farm_no")
    private String farmNo;

    @TableField("farm_name")
    private String farmName;

    @TableField("owner_name")
    private String ownerName;

    @TableField("phone")
    private String phone;

    @TableField("address")
    private String address;

    /** PIG / CATTLE / SHEEP / POULTRY。 */
    @TableField("species")
    private String species;

    @TableField("stock_qty")
    private Integer stockQty;

    /** ACTIVE / SUSPENDED / CLOSED。 */
    @TableField("status")
    private String status;
}
