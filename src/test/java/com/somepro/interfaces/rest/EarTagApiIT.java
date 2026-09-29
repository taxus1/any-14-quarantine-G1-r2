package com.somepro.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 畜禽耳标端到端集成测试（真实 MySQL）。
 *
 * 覆盖：耳标号 ET-年份-6 位且不重号、领标不选种类而照所属场、默认 ISSUED、
 * 改挂场种类跟随、佩戴转 USED 记时刻、遗失/停用不能戴、按场/种类/状态翻页、
 * 每行带耳标号、缴销后消失、存量耳标照样翻得到且不占新年份序号。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EarTagApiIT extends AbstractApiIT {

    private static final String AUTH = "Basic " + Base64.getEncoder()
            .encodeToString("admin:admin123".getBytes(StandardCharsets.UTF_8));

    @Autowired
    private WebTestClient web;

    private final ObjectMapper om = new ObjectMapper();

    private long pigFarmId;
    private long cattleFarmId;
    private long legacyFarmId;

    private JsonNode request(String method, String uri, String json) {
        WebTestClient.RequestBodySpec spec;
        if ("POST".equals(method)) {
            spec = web.post().uri(uri);
        } else if ("PUT".equals(method)) {
            spec = web.put().uri(uri);
        } else {
            throw new IllegalArgumentException(method);
        }
        byte[] body = spec.header(HttpHeaders.AUTHORIZATION, AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange().expectStatus().isOk().expectBody().returnResult().getResponseBody();
        return parse(body);
    }

    private JsonNode getJson(String uri) {
        byte[] body = web.get().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, AUTH)
                .exchange().expectStatus().isOk().expectBody().returnResult().getResponseBody();
        return parse(body);
    }

    private JsonNode parse(byte[] body) {
        try {
            return om.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException(body == null ? "null" : new String(body, StandardCharsets.UTF_8), e);
        }
    }

    private long createFarm(String name, String species) {
        JsonNode resp = request("POST", "/api/farms",
                """
                {"farmName":"IT-%s","species":"%s","stockQty":10}
                """.formatted(name, species));
        assertEquals(0, resp.get("code").asInt(), resp::toString);
        return resp.get("data").get("id").asLong();
    }

    private void ensureFarms() {
        if (pigFarmId == 0) {
            pigFarmId = createFarm("耳标测试猪场", "PIG");
            cattleFarmId = createFarm("耳标测试牛场", "CATTLE");
            legacyFarmId = getJson("/api/farms?farmNo=" + LEGACY_FARM_NO)
                    .get("data").get("content").get(0).get("id").asLong();
        }
    }

    private long issueTag(long farmId) {
        return issueTag(farmId, null);
    }

    private long issueTag(long farmId, String issuedAt) {
        String json = issuedAt == null
                ? "{\"farmId\":%d}".formatted(farmId)
                : "{\"farmId\":%d,\"issuedAt\":\"%s\"}".formatted(farmId, issuedAt);
        JsonNode resp = request("POST", "/api/ear-tags", json);
        assertEquals(0, resp.get("code").asInt(), resp::toString);
        JsonNode d = resp.get("data");
        assertTrue(d.get("tagNo").asText().matches("ET-\\d{4}-\\d{6}"), "耳标号格式 ET-年份-6位");
        assertEquals("ISSUED", d.get("status").asText(), "刚领的默认已发放待戴");
        assertFalse(d.get("issuedAt").isNull(), "发放时刻要有");
        return d.get("id").asLong();
    }

    @Test
    @Order(0)
    void legacyTagRemainsReadableAndNewYearStartsAtOne() {
        ensureFarms();
        // 存量 2025 耳标能查到，字段原样
        JsonNode byNo = getJson("/api/ear-tags?tagNo=" + LEGACY_TAG_NO);
        assertEquals(1, byNo.get("data").get("total").asLong(), "存量耳标照样翻得出来");
        JsonNode legacy = byNo.get("data").get("content").get(0);
        assertEquals(legacyFarmId, legacy.get("farmId").asLong());
        assertEquals("SHEEP", legacy.get("species").asText(), "存量耳标种类跟随存量场");
        assertEquals("ISSUED", legacy.get("status").asText());
    }

    @Test
    @Order(1)
    void issueFollowsFarmSpeciesAndNumbersSequentially() {
        ensureFarms();
        long t1 = issueTag(pigFarmId);
        long t2 = issueTag(cattleFarmId);
        JsonNode d1 = getJson("/api/ear-tags/" + t1).get("data");
        JsonNode d2 = getJson("/api/ear-tags/" + t2).get("data");
        assertTrue(d1.get("tagNo").asText().startsWith("ET-2026-000001"),
                "2025 存量号不挤占，当年第一枚是 000001，实际：" + d1.get("tagNo").asText());
        assertEquals(pigFarmId, d1.get("farmId").asLong());
        assertEquals("PIG", d1.get("species").asText(), "场里定 PIG，耳标就是 PIG");
        assertEquals("CATTLE", d2.get("species").asText(), "挂到牛场就照牛的种类");
        assertNotEquals(d1.get("tagNo").asText(), d2.get("tagNo").asText(), "两枚不能共用一个号");
        int s1 = Integer.parseInt(d1.get("tagNo").asText().substring(8));
        int s2 = Integer.parseInt(d2.get("tagNo").asText().substring(8));
        assertEquals(s1 + 1, s2, "年内顺序号递增");
    }

    @Test
    @Order(2)
    void issueToMissingFarmRejected() {
        JsonNode resp = request("POST", "/api/ear-tags", "{\"farmId\":999999999}");
        assertNotEquals(0, resp.get("code").asInt(), "挂到不存在的场必须报错");
    }

    @Test
    @Order(3)
    void wearRecordsTimeAndStatus() {
        ensureFarms();
        long id = issueTag(pigFarmId, "2026-03-01 09:00:00");
        JsonNode resp = request("PUT", "/api/ear-tags/" + id, """
                {"status":"USED","wornAt":"2026-03-02 10:30:00"}""");
        assertEquals(0, resp.get("code").asInt(), resp::toString);
        JsonNode d = resp.get("data");
        assertEquals("USED", d.get("status").asText());
        assertEquals("2026-03-02 10:30:00", d.get("wornAt").asText());
        assertEquals("PIG", d.get("species").asText());
    }

    @Test
    @Order(4)
    void lostTagCannotBeWorn() {
        ensureFarms();
        long id = issueTag(pigFarmId);
        assertEquals(0, request("PUT", "/api/ear-tags/" + id, """
                {"status":"LOST"}""").get("code").asInt());
        JsonNode wear = request("PUT", "/api/ear-tags/" + id, """
                {"status":"USED"}""");
        assertNotEquals(0, wear.get("code").asInt(), "遗失的不能登记佩戴");
    }

    @Test
    @Order(5)
    void moveToAnotherFarmSyncsSpecies() {
        ensureFarms();
        long id = issueTag(pigFarmId);
        JsonNode resp = request("PUT", "/api/ear-tags/" + id,
                "{\"farmId\":%d}".formatted(cattleFarmId));
        assertEquals(0, resp.get("code").asInt(), resp::toString);
        JsonNode d = resp.get("data");
        assertEquals(cattleFarmId, d.get("farmId").asLong());
        assertEquals("CATTLE", d.get("species").asText(), "改挂牛场，种类自动跟着变 CATTLE");
        assertEquals("ISSUED", d.get("status").asText(), "其它字段不受影响");
    }

    @Test
    @Order(6)
    void pageByFarmSpeciesStatusEveryRowHasTagNo() {
        ensureFarms();
        long tA = issueTag(pigFarmId);
        long tB = issueTag(pigFarmId);
        request("PUT", "/api/ear-tags/" + tA, """
                {"status":"USED"}""");
        request("PUT", "/api/ear-tags/" + tB, """
                {"status":"DISABLED"}""");

        JsonNode byFarm = getJson("/api/ear-tags?farmId=" + pigFarmId + "&pageNum=1&pageSize=100");
        assertTrue(byFarm.get("data").get("total").asLong() >= 3);
        byFarm.get("data").get("content").forEach(n -> {
            assertEquals(pigFarmId, n.get("farmId").asLong());
            assertTrue(n.get("tagNo").asText().matches("ET-\\d{4}-\\d{6}"), "每行都要带耳标号");
        });

        JsonNode used = getJson("/api/ear-tags?status=USED&species=PIG&pageSize=100");
        used.get("data").get("content").forEach(n -> {
            assertEquals("USED", n.get("status").asText());
            assertEquals("PIG", n.get("species").asText());
        });

        // 全量名册包含存量耳标和本套件耳标
        JsonNode all = getJson("/api/ear-tags?pageNum=1&pageSize=200");
        assertTrue(all.get("data").get("total").asLong() >= 5, "条件都不填返回全量（含存量）");
        assertTrue(all.get("data").get("content").findValuesAsText("tagNo").contains(LEGACY_TAG_NO));
    }

    @Test
    @Order(7)
    void paginationNoOverlap() {
        ensureFarms();
        for (int i = 0; i < 4; i++) {
            issueTag(pigFarmId);
        }
        JsonNode p1 = getJson("/api/ear-tags?farmId=" + pigFarmId + "&pageNum=1&pageSize=2");
        JsonNode p2 = getJson("/api/ear-tags?farmId=" + pigFarmId + "&pageNum=2&pageSize=2");
        var i1 = p1.get("data").get("content").findValuesAsText("id");
        var i2 = p2.get("data").get("content").findValuesAsText("id");
        assertEquals(2, i1.size());
        assertTrue(i1.stream().noneMatch(i2::contains), "翻页不能重样");
    }

    @Test
    @Order(8)
    void deleteRemovesFromRoster() {
        ensureFarms();
        long id = issueTag(pigFarmId);
        byte[] body = web.delete().uri("/api/ear-tags/" + id)
                .header(HttpHeaders.AUTHORIZATION, AUTH)
                .exchange().expectStatus().isOk().expectBody().returnResult().getResponseBody();
        assertEquals(0, parse(body).get("code").asInt());
        assertNotEquals(0, getJson("/api/ear-tags/" + id).get("code").asInt());
        JsonNode page = getJson("/api/ear-tags?farmId=" + pigFarmId + "&pageSize=100");
        assertFalse(page.get("data").get("content").findValuesAsText("id")
                .contains(String.valueOf(id)), "缴销后不该再从名单翻出来");
        byte[] again = web.delete().uri("/api/ear-tags/" + id)
                .header(HttpHeaders.AUTHORIZATION, AUTH)
                .exchange().expectStatus().isOk().expectBody().returnResult().getResponseBody();
        assertNotEquals(0, parse(again).get("code").asInt(), "重复缴销应业务失败");
    }
}
