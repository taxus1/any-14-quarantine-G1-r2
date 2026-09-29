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
 * t_ear_tag 畜禽耳标表的持久化对象（PO，基础设施层）。
 *
 * 字段与列一一对应，不放业务规则（规则在领域对象 EarTag）；
 * 枚举在 PO 侧以 String 代码落库，互转见 EarTagPoConverter。
 */
@Getter
@Setter
@TableName("t_ear_tag")
public class EarTagPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 耳标号，全局唯一 ET-2026-000001 */
    @TableField("tag_no")
    private String tagNo;

    @TableField("farm_id")
    private Long farmId;

    /** PIG / CATTLE / SHEEP / POULTRY，跟随所属场 */
    @TableField("species")
    private String species;

    @TableField("issued_at")
    private LocalDateTime issuedAt;

    @TableField("worn_at")
    private LocalDateTime wornAt;

    /** ISSUED / USED / LOST / DISABLED */
    @TableField("status")
    private String status;
}
