package com.somepro.domain.eartag.model;

import com.somepro.common.exception.BizException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 耳标编号（领域值对象）：格式 {@code ET-2026-000001}。
 *
 * 规则：前缀 ET + 年份 + 年内 6 位顺序号（不足左补 0），全库唯一。
 * 顺序号由仓储层统计当年已占用的最大序号得出（含已软删记录，避免销号后重号）。
 */
public final class TagNo {

    /** 编号前缀 */
    public static final String PREFIX = "ET";

    /** 顺序号位数 */
    public static final int SEQ_WIDTH = 6;

    private static final Pattern PATTERN = Pattern.compile("^ET-(\\d{4})-(\\d+)$");

    private TagNo() {
    }

    /** 按年份与年内顺序号拼编号。 */
    public static String format(int year, long seq) {
        if (seq <= 0) {
            throw new BizException("耳标编号顺序号必须为正数");
        }
        return String.format("%s-%d-%0" + SEQ_WIDTH + "d", PREFIX, year, seq);
    }

    /** 解析编号尾部的年内顺序号；不符合格式返回 0。 */
    public static long parseSeq(String tagNo, int year) {
        if (tagNo == null) {
            return 0;
        }
        Matcher m = PATTERN.matcher(tagNo.trim());
        if (!m.matches() || Integer.parseInt(m.group(1)) != year) {
            return 0;
        }
        return Long.parseLong(m.group(2));
    }
}
