package com.somepro.domain.eartag.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import com.somepro.domain.shared.model.Species;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 畜禽耳标聚合根（领域层）。
 *
 * 一枚耳标一条记录；耳标号 tagNo 由仓储按「ET-年份-6 位顺序号」生成。
 * 领标时不选种类，种类照所属养殖场走 —— 挂到哪家场就锁定为那家场的种类。
 */
@Getter
@Setter
public class EarTag extends BaseEntity {

    private Long id;

    /** 耳标号，全局唯一，如 ET-2026-000001；新建时由仓储分配。 */
    private String tagNo;

    /** 挂在哪家场（t_farm.id） */
    private Long farmId;

    /** 种类，跟随所属养殖场，不在领标时单独选择 */
    private Species species;

    /** 发放时刻 */
    private LocalDateTime issuedAt;

    /** 佩戴时刻 */
    private LocalDateTime wornAt;

    /** 状态：ISSUED 已发放待佩戴 / USED 已佩戴 / LOST 遗失 / DISABLED 停用 */
    private EarTagStatus status;

    /**
     * 工厂方法：发放耳标。
     *
     * @param species 所属养殖场的种类（由应用层查场后带入，不做跨聚合查询）
     */
    public static EarTag issue(Long farmId, Species species, LocalDateTime issuedAt) {
        EarTag tag = new EarTag();
        tag.attachTo(farmId, species);
        tag.issuedAt = issuedAt == null ? LocalDateTime.now() : issuedAt;
        tag.status = EarTagStatus.ISSUED;
        return tag;
    }

    /**
     * 领域行为：归属到某家场，种类照该场走（领标挂场 / 后续改挂都走这里）。
     */
    public void attachTo(Long farmId, Species species) {
        if (farmId == null) {
            throw new BizException("耳标必须挂在某个养殖场名下");
        }
        if (species == null) {
            throw new BizException("耳标种类由所属养殖场带出，不能为空");
        }
        this.farmId = farmId;
        this.species = species;
    }

    /**
     * 领域行为：登记佩戴。只有「已发放待佩戴」的耳标能戴，遗失/停用的不能再戴。
     */
    public void markWorn(LocalDateTime wornAt) {
        if (this.status == EarTagStatus.LOST || this.status == EarTagStatus.DISABLED) {
            throw new BizException("当前耳标状态为" + this.status + "，不能登记佩戴");
        }
        this.status = EarTagStatus.USED;
        this.wornAt = wornAt == null ? LocalDateTime.now() : wornAt;
    }

    /** 领域行为：改状态（报失、停用、恢复待戴等）。 */
    public void changeStatus(EarTagStatus target) {
        if (target == null) {
            throw new BizException("耳标状态不能为空");
        }
        this.status = target;
    }
}
