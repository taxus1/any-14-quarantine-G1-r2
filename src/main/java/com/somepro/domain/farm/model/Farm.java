package com.somepro.domain.farm.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import com.somepro.domain.shared.model.Change;
import com.somepro.domain.shared.model.Species;
import lombok.Getter;
import lombok.Setter;

/**
 * 养殖场档案聚合根（一个场一条记录，纯领域对象，不带任何持久化注解）。
 *
 * 不变量：
 * - 场编号（farmNo）全局唯一，由仓储层按 {@link FarmNo} 规则生成后回填，不由调用方指定；
 * - 场名、养殖种类不能为空；
 * - 新立档案状态默认 {@link FarmStatus#ACTIVE}，存栏默认 0。
 */
@Getter
@Setter
public class Farm extends BaseEntity {

    private Long id;

    /** 场编号，如 FM-2026-0001，全局唯一。 */
    private String farmNo;

    /** 场名。 */
    private String farmName;

    /** 负责人。 */
    private String ownerName;

    /** 联系方式。 */
    private String phone;

    /** 场址。 */
    private String address;

    /** 养殖种类：PIG / CATTLE / SHEEP / POULTRY 四选一。 */
    private Species species;

    /** 当前存栏数。 */
    private Integer stockQty;

    /** 场状态：ACTIVE / SUSPENDED / CLOSED，新立默认 ACTIVE。 */
    private FarmStatus status;

    /**
     * 工厂方法：新立场档案。编号留给仓储层生成后 {@link #assignNo} 回填。
     */
    public static Farm register(String farmName, String ownerName, String phone, String address,
                                Species species, Integer stockQty) {
        Farm farm = new Farm();
        farm.changeName(farmName);
        farm.setOwnerName(normalize(ownerName));
        farm.setPhone(normalize(phone));
        farm.setAddress(normalize(address));
        farm.changeSpecies(species);
        if (stockQty != null && stockQty < 0) {
            throw new BizException("存栏数不能为负");
        }
        farm.setStockQty(stockQty == null ? 0 : stockQty);
        farm.status = FarmStatus.ACTIVE;
        return farm;
    }

    /** 仓储层生成编号后回填；不允许改成另一个已用编号。 */
    public void assignNo(String farmNo) {
        if (this.farmNo != null) {
            throw new BizException("场编号已存在，不允许修改");
        }
        this.farmNo = farmNo;
    }

    /**
     * 仓储层专用：编号撞唯一键需要换号重试时覆盖暂存编号。
     * 只有仓储实现会在落库成功前调用，业务用例没有改号入口。
     */
    public void reassignNoForRetry(String farmNo) {
        this.farmNo = farmNo;
    }

    /** 改场名（场名是台账关键字段，不允许改空）。 */
    public void changeName(String farmName) {
        if (farmName == null || farmName.isBlank()) {
            throw new BizException("场名不能为空");
        }
        this.farmName = farmName.trim();
    }

    /** 改养殖种类（四选一，不允许置空）。 */
    public void changeSpecies(Species species) {
        if (species == null) {
            throw new BizException("养殖种类不能为空，仅支持 PIG / CATTLE / SHEEP / POULTRY");
        }
        this.species = species;
    }

    /**
     * 修改档案（部分更新）：每个字段用 {@link Change} 区分三态 ——
     * absent 没传保持原值；clear 传空串清空可空字段；set 改为指定值。
     * 场名、负责人、联系方式、场址、存栏、状态都允许改；
     * 种类同样允许在场档案上更正（耳标种类只在领用当下随场，不做级联）。
     */
    public void edit(Change<String> farmName, Change<String> ownerName, Change<String> phone,
                     Change<String> address, Change<Species> species,
                     Change<Integer> stockQty, Change<FarmStatus> status) {
        farmName.ifPresent(this::changeName);
        ownerName.ifPresent(v -> this.ownerName = normalize(v));
        phone.ifPresent(v -> this.phone = normalize(v));
        address.ifPresent(v -> this.address = normalize(v));
        species.ifPresent(s -> this.species = s);
        stockQty.ifPresent(q -> {
            if (q != null && q < 0) {
                throw new BizException("存栏数不能为负");
            }
            this.stockQty = q;
        });
        status.ifPresent(s -> this.status = s);
    }

    /** 空白字符串规整为 null（可空字段允许「清空」，不允许塞进一串空格）。 */
    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
