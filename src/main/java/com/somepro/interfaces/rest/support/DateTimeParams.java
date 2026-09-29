package com.somepro.interfaces.rest.support;

import com.somepro.common.exception.BizException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 接口层时间参数解析：兼容三种纸面上常见的填法，
 * 「yyyy-MM-dd HH:mm:ss」（与全局 Jackson 输出格式一致）、ISO 日期时间、纯日期（按 00:00:00 计）。
 *
 * 只做协议适配，应用层拿到的永远是 {@link LocalDateTime}。空白按未传（null）处理。
 */
public final class DateTimeParams {

    private DateTimeParams() {
    }

    public static LocalDateTime parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            if (value.length() == 10) {
                return LocalDateTime.of(LocalDate.parse(value), LocalTime.MIN);
            }
            if (value.contains(" ")) {
                return LocalDateTime.parse(value.replace(" ", "T"));
            }
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            throw new BizException("时间格式不合法，应为 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd");
        }
    }
}
