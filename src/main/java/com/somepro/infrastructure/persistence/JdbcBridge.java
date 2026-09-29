package com.somepro.infrastructure.persistence;

import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * 阻塞 JDBC → 响应式链路的共享桥接器（基础设施层）。
 *
 * 各模块仓储适配器继承它即可，桥接顺序是硬约定（见 README「响应式 × 阻塞 JDBC 桥接约定」）：
 * 1. 先在响应式线程上从 Reactor Context 取操作人（切线程后就读不到了）；
 * 2. 再 subscribeOn(boundedElastic) 切到阻塞线程池执行 JDBC，绝不能阻塞 Netty event-loop；
 * 3. 把操作人放进 AuditContextHolder，供 MetaObjectHandler 填 createBy / updateBy。
 */
public abstract class JdbcBridge {

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
}
