package com.somepro.domain.farm.model;

import com.somepro.common.exception.BizException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 养殖场编号（领域值对象）：格式 {@code FM-2026-0001}。
 *
 * 规则：前缀 FM + 年份 + 年内 4 位顺序号（不足左补 0），全库唯一。
 * 顺序号由仓储层统计当年已占用的最大序号得出（含已软删的档案，避免销号后重号），
 * 这里只负责「格式化」与「解析」，不查库。
 */
public final class FarmNo {

    /** 编号前缀 */
    public static final String PREFIX = "FM";

    /** 顺序号位数 */
    public static final int SEQ_WIDTH = 4;

    private static final Pattern PATTERN = Pattern.compile("^FM-(\\d{4})-(\\d+)$");

    private FarmNo() {
    }

    /** 按年份与年内顺序号拼编号。 */
    public static String format(int year, long seq) {
        if (seq <= 0) {
            throw new BizException("养殖场编号顺序号必须为正数");
        }
        return String.format("%s-%d-%0" + SEQ_WIDTH + "d", PREFIX, year, seq);
    }

    /** 解析编号尾部的年内顺序号；不符合格式返回 0（说明库里没有同形态编号，新号从 1 起编）。 */
    public static long parseSeq(String farmNo, int year) {
        if (farmNo == null) {
            return 0;
        }
        Matcher m = PATTERN.matcher(farmNo.trim());
        if (!m.matches() || Integer.parseInt(m.group(1)) != year) {
            return 0;
        }
        return Long.parseLong(m.group(2));
    }
}
