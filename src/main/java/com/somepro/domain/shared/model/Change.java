package com.somepro.domain.shared.model;

import java.util.function.Function;

/**
 * 字段更新三态（领域共享值对象）：部分更新时区分
 *  - {@link #absent()} 没传该参数 → 保持原值；
 *  - {@link #clear()}  显式清空（传了空串）→ 置 null（仅可空字段）；
 *  - {@link #set(Object)} 传了正常值 → 改成该值。
 *
 * 只用 Optional 装不下「清空 null」与「没传」两种 null 语义，故需要 present 这一位。
 * record 不可变、纯数据、无副作用，正适合值对象。
 */
public record Change<T>(boolean present, T value) {

    public static <T> Change<T> absent() {
        return new Change<>(false, null);
    }

    public static <T> Change<T> clear() {
        return new Change<>(true, null);
    }

    public static <T> Change<T> set(T value) {
        return new Change<>(true, value);
    }

    /** 仅当字段「有提供」时消费其值（清空时值为 null）；缺键不做任何事。 */
    public void ifPresent(java.util.function.Consumer<? super T> action) {
        if (present) {
            action.accept(value);
        }
    }

    /** 对「已提供」的值做一次映射：缺键仍是 absent，清空（映射得 null）仍是 clear。 */
    public <R> Change<R> map(Function<? super T, ? extends R> fn) {
        if (!present) {
            return absent();
        }
        R mapped = fn.apply(value);
        return mapped == null ? clear() : set(mapped);
    }
}
