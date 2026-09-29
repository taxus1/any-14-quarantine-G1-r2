package com.somepro.infrastructure.persistence.eartag;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagStatus;
import com.somepro.domain.eartag.repository.EarTagRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import com.somepro.infrastructure.persistence.base.AbstractBlockingRepository;
import com.somepro.infrastructure.persistence.eartag.converter.EarTagPoConverter;
import com.somepro.infrastructure.persistence.eartag.po.EarTagPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 耳标仓储适配器（基础设施层）：MyBatis-Plus 阻塞实现 + 响应式桥接。
 *
 * 耳标号规则：ET-{年份}-{6 位顺序号}，年份内顺序递增。
 * 取号查 MAX（含已软删记录，号不复用），唯一键兜底，撞号重取，保证不重号。
 */
@Repository
public class EarTagRepositoryImpl extends AbstractBlockingRepository implements EarTagRepository {

    /** 耳标号顺序号位数，例：000001 */
    private static final int SEQ_LENGTH = 6;
    private static final int MAX_RETRY = 5;

    private final EarTagMapper earTagMapper;

    public EarTagRepositoryImpl(EarTagMapper earTagMapper) {
        this.earTagMapper = earTagMapper;
    }

    @Override
    public Mono<EarTag> save(EarTag earTag) {
        if (earTag.getId() != null) {
            return blocking(() -> doUpdate(earTag));
        }
        return blocking(() -> {
            EarTagPO po = EarTagPoConverter.toPo(earTag);
            po.setId(IdUtil.getSnowflakeNextId());
            String prefix = "ET-" + LocalDate.now().getYear() + "-";
            for (int attempt = 0; attempt <= MAX_RETRY; attempt++) {
                po.setTagNo(nextTagNo(prefix));
                try {
                    earTagMapper.insert(po);
                    return EarTagPoConverter.toDomain(po);
                } catch (RuntimeException e) {
                    if (isDuplicateKey(e) && attempt < MAX_RETRY) {
                        continue;
                    }
                    throw e;
                }
            }
            throw new BizException("耳标号分配冲突，重试多次仍失败");
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
    public Mono<PageResult<EarTag>> page(int pageNum, int pageSize,
                                         Long farmId, Species species, EarTagStatus status, String tagNo) {
        return this.<PageResult<EarTag>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<EarTagPO> wrapper = Wrappers.<EarTagPO>lambdaQuery()
                        .eq(farmId != null, EarTagPO::getFarmId, farmId)
                        .eq(species != null, EarTagPO::getSpecies, species == null ? null : species.name())
                        .eq(status != null, EarTagPO::getStatus, status == null ? null : status.name())
                        .like(tagNo != null && !tagNo.isBlank(), EarTagPO::getTagNo, tagNo)
                        // 固定按 id 升序，保证翻页稳定、两页之间不重样
                        .orderByAsc(EarTagPO::getId);
                List<EarTagPO> rows = earTagMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<EarTag> content = rows.stream()
                        .map(EarTagPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<Boolean> softDelete(Long id) {
        return blocking(() -> earTagMapper.deleteById(id) > 0);
    }

    private EarTag doUpdate(EarTag earTag) {
        EarTagPO po = EarTagPoConverter.toPo(earTag);
        earTagMapper.updateById(po);
        EarTagPO fresh = earTagMapper.selectById(po.getId());
        return fresh == null ? EarTagPoConverter.toDomain(po) : EarTagPoConverter.toDomain(fresh);
    }

    private String nextTagNo(String prefix) {
        String max = earTagMapper.selectMaxTagNo(escapeLike(prefix));
        int seq = 1;
        if (max != null && max.length() >= prefix.length() + SEQ_LENGTH) {
            String seqPart = max.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return prefix + String.format("%0" + SEQ_LENGTH + "d", seq);
    }
}
