package com.somepro.infrastructure.persistence.farm;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.model.FarmStatus;
import com.somepro.domain.farm.repository.FarmRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import com.somepro.infrastructure.persistence.base.AbstractBlockingRepository;
import com.somepro.infrastructure.persistence.farm.converter.FarmPoConverter;
import com.somepro.infrastructure.persistence.farm.po.FarmPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 养殖场仓储适配器（基础设施层）：MyBatis-Plus 阻塞实现 + 响应式桥接。
 *
 * 场编号规则：FM-{年份}-{4 位顺序号}，年份内顺序递增。
 * 取号查 MAX（含已软删记录，号不复用），唯一键兜底，撞号重取，保证不重号。
 */
@Repository
public class FarmRepositoryImpl extends AbstractBlockingRepository implements FarmRepository {

    /** 场编号顺序号位数，例：0001 */
    private static final int SEQ_LENGTH = 4;
    private static final int MAX_RETRY = 5;

    private final FarmMapper farmMapper;

    public FarmRepositoryImpl(FarmMapper farmMapper) {
        this.farmMapper = farmMapper;
    }

    @Override
    public Mono<Farm> save(Farm farm) {
        if (farm.getId() != null) {
            // 更新无需取号
            return blocking(() -> doUpdate(farm));
        }
        return blocking(() -> {
            FarmPO po = FarmPoConverter.toPo(farm);
            po.setId(IdUtil.getSnowflakeNextId());
            String prefix = "FM-" + LocalDate.now().getYear() + "-";
            // 并发领号时唯一键兜底：撞号就重新 MAX + 1 再插
            for (int attempt = 0; attempt <= MAX_RETRY; attempt++) {
                po.setFarmNo(nextFarmNo(prefix));
                try {
                    farmMapper.insert(po);
                    return FarmPoConverter.toDomain(po);
                } catch (RuntimeException e) {
                    if (isDuplicateKey(e) && attempt < MAX_RETRY) {
                        continue;
                    }
                    throw e;
                }
            }
            throw new BizException("场编号分配冲突，重试多次仍失败");
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
                        // 固定按 id 升序，保证翻页稳定、两页之间不重样
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
                // 分页参数走 ThreadLocal，必须清理，避免污染线程池里的下一次调用
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<Boolean> softDelete(Long id) {
        return blocking(() -> {
            // @TableLogic 翻译成 UPDATE t_farm SET del_flag = 1 WHERE id = ? AND del_flag = 0
            return farmMapper.deleteById(id) > 0;
        });
    }

    private Farm doUpdate(Farm farm) {
        FarmPO po = FarmPoConverter.toPo(farm);
        farmMapper.updateById(po);
        // 回填审计字段后返回库里的最新形状
        FarmPO fresh = farmMapper.selectById(po.getId());
        return fresh == null ? FarmPoConverter.toDomain(po) : FarmPoConverter.toDomain(fresh);
    }

    /**
     * 基于年度前缀取一个新场编号：当前最大号 + 1；年内第一个则是 0001。
     * 前缀里的下划线已转义，查询不会误匹配其它编号。
     */
    private String nextFarmNo(String prefix) {
        String max = farmMapper.selectMaxFarmNo(escapeLike(prefix));
        int seq = 1;
        if (max != null && max.length() >= prefix.length() + SEQ_LENGTH) {
            String seqPart = max.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException ignored) {
                // 历史脏数据尾段不是数字时，从格式正确的号段之外另起；正常不会走到
                seq = 1;
            }
        }
        return prefix + String.format("%0" + SEQ_LENGTH + "d", seq);
    }
}
