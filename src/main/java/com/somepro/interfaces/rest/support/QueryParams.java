package com.somepro.interfaces.rest.support;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.Change;
import org.springframework.web.server.ServerWebExchange;

import java.util.Optional;

/**
 * 接口层查询参数提取工具：把原始 query 参数转成领域三态 {@link Change}。
 *
 * 背景：WebFlux 下 {@code @RequestParam(required=false)} 把「参数没传」和「参数传了空串」
 * 都解析成 null（MultiValueMap 里前者无 key、后者 key 对应空串，注解不区分）。
 * 但档案修改语义要求：缺键 → 保持原值（absent）；空串 → 清空可空字段（clear）；正常值 → set。
 */
public final class QueryParams {

    private QueryParams() {
    }

    /** 字符串字段：缺键不改；有键（含空串）按原值改，空白由领域层规整为 null。 */
    public static Change<String> change(ServerWebExchange exchange, String name) {
        return Optional.ofNullable(exchange.getRequest().getQueryParams().getFirst(name))
                .map(Change::set)
                .orElseGet(Change::absent);
    }

    /** 整数字段：缺键不改；空串清空；非空但非法按业务异常提示（不抛 500）。 */
    public static Change<Integer> changeInt(ServerWebExchange exchange, String name) {
        return change(exchange, name).map(raw -> {
            if (raw.isBlank()) {
                return null;
            }
            try {
                return Integer.valueOf(raw.trim());
            } catch (NumberFormatException e) {
                throw new BizException("参数 " + name + " 必须是整数");
            }
        });
    }

    /** 长整数字段（如 farmId），规则同 {@link #changeInt}。 */
    public static Change<Long> changeLong(ServerWebExchange exchange, String name) {
        return change(exchange, name).map(raw -> {
            if (raw.isBlank()) {
                return null;
            }
            try {
                return Long.valueOf(raw.trim());
            } catch (NumberFormatException e) {
                throw new BizException("参数 " + name + " 必须是整数");
            }
        });
    }

    /** 时间字段：缺键不改；空串清空；非空按 {@link DateTimeParams} 解析。 */
    public static Change<java.time.LocalDateTime> changeDateTime(ServerWebExchange exchange, String name) {
        return change(exchange, name).map(raw -> raw.isBlank() ? null : DateTimeParams.parse(raw));
    }
}
