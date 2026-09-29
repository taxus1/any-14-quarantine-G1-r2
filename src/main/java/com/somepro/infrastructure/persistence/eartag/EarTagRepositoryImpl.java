package com.somepro.infrastructure.persistence.eartag;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagStatus;
import com.somepro.domain.eartag.model.EarTagView;
import com.somepro.domain.eartag.model.TagNo;
import com.somepro.domain.eartag.repository.EarTagRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import com.somepro.infrastructure.persistence.JdbcBridge;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.eartag.converter.EarTagPoConverter;
import com.somepro.infrastructure.persistence.eartag.po.EarTagPO;
import com.somepro.infrastructure.persistence.farm.FarmMapper;
import com.somepro.infrastructure.persistence.farm.po.FarmPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 畜禽耳标仓储适配器：MyBatis-Plus 实现 {@link EarTagRepository}（基础设施层）。
 *
 * - 耳标号按 {@link TagNo} 规则生成，取号含软删行，唯一键 uk_tag_no 兜底并发（重试换号）；
 * - 分页名册按场/种类/状态任意组合，查出后批量补场编号 farmNo，每行都带耳标号；
 * - 软删交给 @TableLogic；所有 JDBC 调用经 {@link #blocking} 桥接到 boundedElastic。
 */
@Repository
public class EarTagRepositoryImpl extends JdbcBridge implements EarTagRepository {

    /** 编号唯一键冲突时的取号重试上限。 */
    private static final int NO_RETRY_MAX = 3;

    private final EarTagMapper earTagMapper;
    private final FarmMapper farmMapper;

    public EarTagRepositoryImpl(EarTagMapper earTagMapper, FarmMapper farmMapper) {
        this.earTagMapper = earTagMapper;
        this.farmMapper = farmMapper;
    }

    @Override
    public Mono<EarTag> create(EarTag earTag) {
        return blocking(() -> {
            for (int attempt = 1; attempt <= NO_RETRY_MAX; attempt++) {
                int year = LocalDate.now().getYear();
                String tagNo = nextTagNo(year);
                if (earTag.getTagNo() == null) {
                    earTag.assignNo(tagNo);
                } else {
                    earTag.reassignNoForRetry(tagNo);
                }
                EarTagPO po = EarTagPoConverter.toPo(earTag);
                po.setId(IdUtil.getSnowflakeNextId());
                try {
                    earTagMapper.insert(po);
                    return EarTagPoConverter.toDomain(po);
                } catch (Exception e) {
                    // 只对编号唯一键冲突重试换号（雪花 id 也一并重取）；其它异常照常抛出
                    if (attempt < NO_RETRY_MAX && isDuplicateKey(e)) {
                        continue;
                    }
                    throw e;
                }
            }
            throw new BizException("耳标号生成冲突，连续 " + NO_RETRY_MAX + " 次撞号，请重试");
        });
    }

    @Override
    public Mono<EarTag> update(EarTag earTag) {
        return blocking(() -> {
            // 全字段 set（含可空的 issued_at / worn_at）：时刻录错允许改正/清空。
            // UpdateWrapper.set(...) 是手写片段，MetaObjectHandler 不介入，审计列显式带上；
            // @TableLogic 仍自动追加 del_flag = 0，注销行改不到。
            LambdaUpdateWrapper<EarTagPO> uw = Wrappers.<EarTagPO>lambdaUpdate()
                    .eq(EarTagPO::getId, earTag.getId())
                    .set(EarTagPO::getFarmId, earTag.getFarmId())
                    .set(EarTagPO::getSpecies, earTag.getSpecies() == null ? null : earTag.getSpecies().name())
                    .set(EarTagPO::getIssuedAt, earTag.getIssuedAt())
                    .set(EarTagPO::getWornAt, earTag.getWornAt())
                    .set(EarTagPO::getStatus, earTag.getStatus() == null ? null : earTag.getStatus().name())
                    .set(EarTagPO::getUpdateBy, AuditContextHolder.getOperator())
                    .set(EarTagPO::getUpdateTime, LocalDateTime.now());
            earTagMapper.update(null, uw);
            EarTagPO latest = earTagMapper.selectById(earTag.getId());
            return latest == null ? null : EarTagPoConverter.toDomain(latest);
        });
    }

    @Override
    public Mono<EarTag> findById(Long id) {
        return blocking(() -> {
            EarTagPO po = earTagMapper.selectById(id);
            return po == null ? null : EarTagPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PageResult<EarTagView>> page(int pageNum, int pageSize,
                                             Long farmId, Species species, EarTagStatus status) {
        return this.<PageResult<EarTagView>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<EarTagPO> wrapper = Wrappers.<EarTagPO>lambdaQuery()
                        .eq(farmId != null, EarTagPO::getFarmId, farmId)
                        .eq(species != null, EarTagPO::getSpecies, species == null ? null : species.name())
                        .eq(status != null, EarTagPO::getStatus, status == null ? null : status.name())
                        .orderByAsc(EarTagPO::getId);
                List<EarTagPO> rows = earTagMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();

                Map<Long, String> farmNoMap = loadFarmNos(rows);
                List<EarTagView> content = rows.stream()
                        .map(po -> EarTagPoConverter.toView(po, farmNoMap.get(po.getFarmId())))
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
            // @TableLogic 翻译成 UPDATE t_ear_tag SET del_flag = 1 WHERE id = ? AND del_flag = 0
            earTagMapper.deleteById(id);
            return Boolean.TRUE;
        }).then();
    }

    /** 批量查本页耳标所属场的编号（一次 IN 查询，不逐行回查）。 */
    private Map<Long, String> loadFarmNos(List<EarTagPO> rows) {
        Set<Long> farmIds = rows.stream().map(EarTagPO::getFarmId).collect(Collectors.toSet());
        if (farmIds.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, String> map = new HashMap<>();
        for (FarmPO farm : farmMapper.selectBatchIds(farmIds)) {
            map.put(farm.getId(), farm.getFarmNo());
        }
        return map;
    }

    /**
     * 生成下一个耳标号：拉取当年前缀（ET-yyyy-）下的全部编号（含软删行），解析序号取最大 +1。
     */
    private String nextTagNo(int year) {
        // '_' 转义成 \_ 只匹配下划线本身，结尾补 % 作为通配尾缀匹配 6 位顺序号。
        String likePrefix = TagNo.PREFIX + "-" + year + "-";
        long maxSeq = earTagMapper.selectAllNosLike(escapeLike(likePrefix) + "%").stream()
                .mapToLong(no -> TagNo.parseSeq(no, year))
                .max()
                .orElse(0L);
        return TagNo.format(year, maxSeq + 1);
    }

    /** 转义 LIKE 里会扩大匹配面的 \ / % / _。 */
    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** 沿异常链识别唯一键冲突（MySQL 1062 / SQLState 23000），不依赖驱动具体异常类型。 */
    private boolean isDuplicateKey(Throwable e) {
        Throwable cur = e;
        int depth = 0;
        while (cur != null && depth++ < 10) {
            String message = cur.getMessage();
            if (message != null && (message.contains("Duplicate entry") || message.contains("uk_tag_no"))) {
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
