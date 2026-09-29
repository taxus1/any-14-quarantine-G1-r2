package com.somepro.infrastructure.persistence.eartag.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * t_ear_tag 表的持久化对象（PO，基础设施层）：字段与列一一对应，不放业务规则。
 */
@Getter
@Setter
@TableName("t_ear_tag")
public class EarTagPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 耳标号，全局唯一，如 ET-2026-000001。 */
    @TableField("tag_no")
    private String tagNo;

    /** 所属养殖场 id（t_farm.id）。 */
    @TableField("farm_id")
    private Long farmId;

    /** 种类随养殖场档案带出：PIG / CATTLE / SHEEP / POULTRY。 */
    @TableField("species")
    private String species;

    @TableField("issued_at")
    private LocalDateTime issuedAt;

    @TableField("worn_at")
    private LocalDateTime wornAt;

    /** ISSUED / USED / LOST / DISABLED。 */
    @TableField("status")
    private String status;
}
