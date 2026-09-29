package com.somepro.interfaces.rest;

import cn.hutool.core.util.IdUtil;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 端到端集成测试基类：直接打验收环境提供的真实 MySQL（any_14_quarantine_b）。
 *
 * 数据纪律（不碰任何非测试数据）：
 * - 不建表、不改表：t_farm / t_ear_tag 已由 doc/schema/quarantine.sql 预建；
 * - 清理一律带 WHERE，只删本套件造的行：测试场名统一带 IT- 前缀，
 *   耳标按其归属测试场的 id 清理（先清耳标再清场，不产生孤儿耳标）；
 * - 另放两条 2025 年份的「存量记录」（无 IT- 前缀、永不清理），
 *   用来验证「库里先前攒下的记录照样得能翻出来」，且不干扰 2026 年取号。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.datasource.url=jdbc:mysql://host.docker.internal:3306/any_14_quarantine_b?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
        "spring.datasource.username=root",
        "spring.datasource.password=root"
})
abstract class AbstractApiIT {

    /** 存量养殖场编号（2025 年份，验证历史数据可读、且不占用 2026 取号序列）。 */
    static final String LEGACY_FARM_NO = "FM-2025-9001";
    /** 存量耳标号（2025 年份）。 */
    static final String LEGACY_TAG_NO = "ET-2025-900001";
    /** 本套件测试数据名前缀；清理只认它。 */
    static final String TEST_NAME_PREFIX = "IT-";

    @BeforeAll
    static void prepareData(@Autowired DataSource ds) throws Exception {
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            // 1) 只清本套件历史残留：先耳标（按测试场 id），再场（按名前缀）。全部带 WHERE。
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("""
                        DELETE FROM t_ear_tag
                        WHERE farm_id IN (SELECT id FROM t_farm WHERE farm_name LIKE 'IT-%')
                        """);
                st.executeUpdate("DELETE FROM t_farm WHERE farm_name LIKE 'IT-%'");
            }
            // 2) 幂等植入一条存量养殖场（不存在才插），随后拿到它的 id
            long legacyFarmId = seedLegacyFarm(conn);
            // 3) 幂等植入一枚存量耳标
            seedLegacyTag(conn, legacyFarmId);
            conn.commit();
        }
    }

    private static long seedLegacyFarm(Connection conn) throws Exception {
        try (PreparedStatement q = conn.prepareStatement(
                "SELECT id FROM t_farm WHERE farm_no = ?")) {
            q.setString(1, LEGACY_FARM_NO);
            try (ResultSet rs = q.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        long id = IdUtil.getSnowflakeNextId();
        try (PreparedStatement ins = conn.prepareStatement("""
                INSERT INTO t_farm
                (id, farm_no, farm_name, owner_name, phone, address, species, stock_qty, status, del_flag,
                 create_by, create_time, update_by, update_time)
                VALUES (?, ?, '既有存量养殖场-勿删', '李四', '13700001111', '老场址', 'SHEEP', 88, 'ACTIVE', 0,
                        'legacy-seed', NOW(), 'legacy-seed', NOW())
                """)) {
            ins.setLong(1, id);
            ins.setString(2, LEGACY_FARM_NO);
            ins.executeUpdate();
        }
        return id;
    }

    private static void seedLegacyTag(Connection conn, long farmId) throws Exception {
        try (PreparedStatement q = conn.prepareStatement(
                "SELECT id FROM t_ear_tag WHERE tag_no = ?")) {
            q.setString(1, LEGACY_TAG_NO);
            try (ResultSet rs = q.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ins = conn.prepareStatement("""
                INSERT INTO t_ear_tag
                (id, tag_no, farm_id, species, issued_at, worn_at, status, del_flag,
                 create_by, create_time, update_by, update_time)
                VALUES (?, ?, ?, 'SHEEP', NOW(), NULL, 'ISSUED', 0,
                        'legacy-seed', NOW(), 'legacy-seed', NOW())
                """)) {
            ins.setLong(1, IdUtil.getSnowflakeNextId());
            ins.setString(2, LEGACY_TAG_NO);
            ins.setLong(3, farmId);
            ins.executeUpdate();
        }
    }
}
