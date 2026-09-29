package com.somepro.domain.farm.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import com.somepro.domain.shared.model.Species;
import lombok.Getter;
import lombok.Setter;

/**
 * 养殖场档案聚合根（领域层）。
 *
 * 一个场一条记录；场编号 farmNo 由仓储按「FM-年份-4 位顺序号」生成，领域不关心生成细节。
 * 纯领域对象：不带任何持久化注解，表映射在基础设施层的 FarmPO。
 */
@Getter
@Setter
public class Farm extends BaseEntity {

    private Long id;

    /** 场编号，全局唯一，如 FM-2026-0001；新建时由仓储分配。 */
    private String farmNo;

    /** 场名 */
    private String farmName;

    /** 负责人 */
    private String ownerName;

    /** 联系方式 */
    private String phone;

    /** 场址 */
    private String address;

    /** 养殖种类 */
    private Species species;

    /** 当前存栏数 */
    private Integer stockQty;

    /** 场状态：ACTIVE 在用 / SUSPENDED 停业 / CLOSED 注销 */
    private FarmStatus status;

    /**
     * 工厂方法：登记新场。新立场默认 ACTIVE 在用。
     *
     * @param stockQty 存栏数，未传按 0 计
     */
    public static Farm register(String farmName, String ownerName, String phone, String address,
                                Species species, Integer stockQty) {
        Farm farm = new Farm();
        farm.changeProfile(farmName, ownerName, phone, address, species, stockQty == null ? 0 : stockQty);
        farm.status = FarmStatus.ACTIVE;
        return farm;
    }

    /**
     * 领域行为：修改档案（场名、负责人、联系方式、场址、种类、存栏数）。
     * 可空字段未传（null）表示不动；传了空白串则清空。
     */
    public void changeProfile(String farmName, String ownerName, String phone, String address,
                              Species species, Integer stockQty) {
        if (farmName != null) {
            String trimmed = farmName.trim();
            if (trimmed.isEmpty()) {
                throw new BizException("养殖场名称不能为空");
            }
            this.farmName = trimmed;
        }
        if (ownerName != null) {
            this.ownerName = normalizeNullable(ownerName);
        }
        if (phone != null) {
            this.phone = normalizeNullable(phone);
        }
        if (address != null) {
            this.address = normalizeNullable(address);
        }
        if (species != null) {
            this.species = species;
        }
        if (stockQty != null) {
            if (stockQty < 0) {
                throw new BizException("存栏数不能为负数");
            }
            this.stockQty = stockQty;
        }
    }

    /** 领域行为：变更业务状态（含停业 SUSPENDED、注销 CLOSED、恢复在用 ACTIVE）。 */
    public void changeStatus(FarmStatus target) {
        if (target == null) {
            throw new BizException("养殖场状态不能为空");
        }
        this.status = target;
    }

    private static String normalizeNullable(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
