package com.somepro.infrastructure.persistence.farm;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.model.FarmNo;
import com.somepro.domain.farm.model.FarmStatus;
import com.somepro.domain.farm.repository.FarmRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import com.somepro.infrastructure.persistence.JdbcBridge;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.farm.converter.FarmPoConverter;
import com.somepro.infrastructure.persistence.farm.po.FarmPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 养殖场档案仓储适配器：MyBatis-Plus 实现 {@link FarmRepository}（基础设施层）。
 *
 * - 场编号在这里按 {@link FarmNo} 规则生成：统计当年已占用最大顺序号（含软删行）+1；
 * - 唯一键 uk_farm_no 兜底并发：撞键时重新取号重试，最多 {@link #NO_RETRY_MAX} 次；
 * - 分页走 PageHelper，条件任意组合，按 id 升序保证翻页不重不漏；
 * - 软删交给 @TableLogic，不手写 del_flag。
 * 所有 JDBC 调用一律经 {@link #blocking} 桥接到 boundedElastic。
 */
@Repository
public class FarmRepositoryImpl extends JdbcBridge implements FarmRepository {

    /** 编号唯一键冲突时的取号重试上限。 */
    private static final int NO_RETRY_MAX = 3;

    private final FarmMapper farmMapper;

    public FarmRepositoryImpl(FarmMapper farmMapper) {
        this.farmMapper = farmMapper;
    }

    @Override
    public Mono<Farm> create(Farm farm) {
        return blocking(() -> {
            for (int attempt = 1; attempt <= NO_RETRY_MAX; attempt++) {
                int year = LocalDate.now().getYear();
                String farmNo = nextFarmNo(year);
                if (farm.getFarmNo() == null) {
                    farm.assignNo(farmNo);
                } else {
                    farm.reassignNoForRetry(farmNo);
                }
                FarmPO po = FarmPoConverter.toPo(farm);
                po.setId(IdUtil.getSnowflakeNextId());
                try {
                    farmMapper.insert(po);
                    return FarmPoConverter.toDomain(po);
                } catch (Exception e) {
                    // 只对编号唯一键冲突重试换号（雪花 id 也一并重取）；其它异常照常抛出
                    if (attempt < NO_RETRY_MAX && isDuplicateKey(e)) {
                        continue;
                    }
                    throw e;
                }
            }
            throw new BizException("场编号生成冲突，连续 " + NO_RETRY_MAX + " 次撞号，请重试");
        });
    }

    @Override
    public Mono<Farm> update(Farm farm) {
        return blocking(() -> {
            // 全字段 set（含可空列）：允许把负责人/电话/场址「改空」。
            // 不用 updateById 的 NOT_NULL 策略；update_by/update_time 这里显式带上，
            // 因为 UpdateWrapper.set(...) 走的是手写片段，MetaObjectHandler 不介入。
            // @TableLogic 仍会自动追加 del_flag = 0，注销行改不到。
            LambdaUpdateWrapper<FarmPO> uw = Wrappers.<FarmPO>lambdaUpdate()
                    .eq(FarmPO::getId, farm.getId())
                    .set(FarmPO::getFarmName, farm.getFarmName())
                    .set(FarmPO::getOwnerName, farm.getOwnerName())
                    .set(FarmPO::getPhone, farm.getPhone())
                    .set(FarmPO::getAddress, farm.getAddress())
                    .set(FarmPO::getSpecies, farm.getSpecies() == null ? null : farm.getSpecies().name())
                    .set(FarmPO::getStockQty, farm.getStockQty())
                    .set(FarmPO::getStatus, farm.getStatus() == null ? null : farm.getStatus().name())
                    .set(FarmPO::getUpdateBy, AuditContextHolder.getOperator())
                    .set(FarmPO::getUpdateTime, LocalDateTime.now());
            farmMapper.update(null, uw);
            // 以库里最新值回读返回（软删行更新不到，返回 null 时上层转业务异常）
            FarmPO latest = farmMapper.selectById(farm.getId());
            return latest == null ? null : FarmPoConverter.toDomain(latest);
        });
    }

    @Override
    public Mono<Farm> findById(Long id) {
        return blocking(() -> {
            FarmPO po = farmMapper.selectById(id);
            return po == null ? null : FarmPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PageResult<Farm>> page(int pageNum, int pageSize,
                                       String farmName, String farmNo, Species species, FarmStatus status) {
        return this.<PageResult<Farm>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<FarmPO> wrapper = Wrappers.<FarmPO>lambdaQuery()
                        .like(farmName != null && !farmName.isBlank(), FarmPO::getFarmName, farmName)
                        .like(farmNo != null && !farmNo.isBlank(), FarmPO::getFarmNo, farmNo)
                        .eq(species != null, FarmPO::getSpecies, species == null ? null : species.name())
                        .eq(status != null, FarmPO::getStatus, status == null ? null : status.name())
                        .orderByAsc(FarmPO::getId);
                List<FarmPO> rows = farmMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<Farm> content = rows.stream()
                        .map(FarmPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<Void> softDelete(Long id) {
        return blocking(() -> {
            // @TableLogic 翻译成 UPDATE t_farm SET del_flag = 1 WHERE id = ? AND del_flag = 0
            farmMapper.deleteById(id);
            return Boolean.TRUE;
        }).then();
    }

    /**
     * 生成下一个场编号：拉取当年前缀（FM-yyyy-）下的全部编号（含软删行），解析序号取最大 +1。
     * 没有任何同形态编号时从 0001 起编。
     */
    private String nextFarmNo(int year) {
        // 前缀里的 '-' 是字面量；'_' 必须转义成 \_ 才只匹配下划线本身（否则会匹配任意单字符），
        // 结尾再补一个 % 作为真正的通配尾缀去匹配顺序号。
        String likePrefix = FarmNo.PREFIX + "-" + year + "-";
        long maxSeq = farmMapper.selectAllNosLike(escapeLike(likePrefix) + "%").stream()
                .mapToLong(no -> FarmNo.parseSeq(no, year))
                .max()
                .orElse(0L);
        return FarmNo.format(year, maxSeq + 1);
    }

    /** 转义 LIKE 里会扩大匹配面的 \ / % / _（三者都不能按通配符解释）。 */
    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** 沿异常链识别唯一键冲突（MySQL 1062 / SQLState 23000），不依赖驱动具体异常类型。 */
    private boolean isDuplicateKey(Throwable e) {
        Throwable cur = e;
        int depth = 0;
        while (cur != null && depth++ < 10) {
            String message = cur.getMessage();
            if (message != null && (message.contains("Duplicate entry") || message.contains("uk_farm_no"))) {
                return true;
            }
            if (cur instanceof java.sql.SQLException sql) {
                if (sql.getErrorCode() == 1062 || "23000".equals(sql.getSQLState())) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }
}
