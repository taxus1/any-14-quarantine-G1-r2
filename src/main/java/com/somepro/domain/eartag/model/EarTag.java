package com.somepro.domain.eartag.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import com.somepro.domain.shared.model.Change;
import com.somepro.domain.shared.model.Species;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 畜禽耳标聚合根（一枚耳标一条记录，纯领域对象，不带任何持久化注解）。
 *
 * 核心不变量（业务原话：领的时候不用选种类，挂到哪家场就照着那家场的种类走）：
 * - 耳标种类由所属养殖场档案带出，应用层保证 species 与该场 species 一致，
 *   本聚合不提供任何「单独改种类」的入口；
 * - 新领耳标状态默认 {@link EarTagStatus#ISSUED}，发放时刻默认领用当下；
 * - 耳标号（tagNo）全局唯一，由仓储层按 {@link TagNo} 规则生成后回填。
 */
@Getter
@Setter
public class EarTag extends BaseEntity {

    private Long id;

    /** 耳标号，如 ET-2026-000001，全局唯一。 */
    private String tagNo;

    /** 所属养殖场 id（t_farm.id）。 */
    private Long farmId;

    /** 畜禽种类：随所属养殖场带出，不单独录入。 */
    private Species species;

    /** 发放时刻。 */
    private LocalDateTime issuedAt;

    /** 佩戴时刻。 */
    private LocalDateTime wornAt;

    /** 耳标状态：ISSUED / USED / LOST / DISABLED，新领默认 ISSUED。 */
    private EarTagStatus status;

    /**
     * 工厂方法：领（发放）一枚新耳标。
     *
     * @param farmId  挂领的养殖场（必须是在场的有效养殖场，由应用层先查档案）
     * @param species 该场的养殖种类（应用层从场档案带出，不由领用页面传）
     */
    public static EarTag issue(Long farmId, Species species, LocalDateTime issuedAt) {
        EarTag tag = new EarTag();
        tag.attachTo(farmId, species);
        tag.issuedAt = issuedAt == null ? LocalDateTime.now() : issuedAt;
        tag.status = EarTagStatus.ISSUED;
        return tag;
    }

    /** 仓储层生成编号后回填。 */
    public void assignNo(String tagNo) {
        if (this.tagNo != null) {
            throw new BizException("耳标号已存在，不允许修改");
        }
        this.tagNo = tagNo;
    }

    /**
     * 仓储层专用：编号撞唯一键需要换号重试时覆盖暂存编号。
     * 只有仓储实现会在落库成功前调用，业务用例没有改号入口。
     */
    public void reassignNoForRetry(String tagNo) {
        this.tagNo = tagNo;
    }

    /**
     * 修改耳标（部分更新）：每个字段用 {@link Change} 区分三态（没传保持 / 空串清空 / 正常值改）。
     *
     * 改挂养殖场时，种类必须跟着新场一起改 —— 两个值由应用层查新场档案后成对传入，
     * 仍然不存在「单独改种类」的入口。状态由非佩戴变为佩戴的瞬间没给佩戴时刻时，自动记为当下；
     * 已佩戴后单独清空 wornAt 不回填（否则时刻永远清不掉）。
     */
    public void edit(Change<Long> farmId, Change<Species> species,
                     Change<LocalDateTime> issuedAt, Change<LocalDateTime> wornAt,
                     Change<EarTagStatus> status) {
        farmId.ifPresent(fid -> {
            if (!species.present() || species.value() == null) {
                throw new BizException("改挂养殖场时必须同时带出新场的养殖种类");
            }
            attachTo(fid, species.value());
        });
        issuedAt.ifPresent(t -> this.issuedAt = t);
        wornAt.ifPresent(t -> this.wornAt = t);
        status.ifPresent(s -> {
            boolean becomingUsed = s == EarTagStatus.USED && this.status != EarTagStatus.USED;
            this.status = s;
            if (becomingUsed && this.wornAt == null) {
                this.wornAt = LocalDateTime.now();
            }
        });
    }

    /** 挂到场：场与种类成对落定。 */
    private void attachTo(Long farmId, Species species) {
        if (farmId == null) {
            throw new BizException("耳标必须挂领给一家养殖场");
        }
        if (species == null) {
            throw new BizException("耳标种类由养殖场档案带出，场种类缺失，无法登记");
        }
        this.farmId = farmId;
        this.species = species;
    }
}
