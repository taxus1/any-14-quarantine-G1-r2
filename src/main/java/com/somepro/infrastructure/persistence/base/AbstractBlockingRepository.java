package com.somepro.infrastructure.persistence.base;

import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * 仓储适配器公共基类（基础设施层）。
 *
 * 把 demo 里那套「响应式外壳 × 阻塞 JDBC」桥接收敛到一处，两个新模块共用，
 * 顺序与红线一条不能变：
 * 1. 先 deferContextual 从 Reactor Context 取操作人（切线程后就读不到了）；
 * 2. 再 subscribeOn(boundedElastic) 切到阻塞线程池，绝不在 Netty event-loop 上跑 JDBC；
 * 3. 操作人放进 AuditContextHolder 供 MetaObjectHandler 填充，finally 里清掉。
 */
public abstract class AbstractBlockingRepository {

    protected <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }

    /**
     * 是否唯一键冲突。Spring 会把 MySQL 的 1062 翻译成 {@link DuplicateKeyException}；
     * 个别场景下若底层异常未被翻译，则再按错误码/错误信息兜一道，保证单号竞争时能识别并重新取号。
     */
    protected boolean isDuplicateKey(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof DuplicateKeyException) {
                return true;
            }
            String message = cur.getMessage();
            if (message != null && (message.contains("Duplicate entry") || message.contains("1062"))) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    /**
     * 转义 MySQL LIKE 的通配符（%、_）与转义符本身（\）。
     *
     * 单号前缀形如 FM-2026- 里含下划线，不转义时 _ 是「任意单字符」通配，
     * 可能把同样位置是别的字符的编号也卷进 MAX()。配套 SQL 需声明 ESCAPE '\\'。
     */
    protected String escapeLike(String literal) {
        if (literal == null) {
            return null;
        }
        return literal.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
