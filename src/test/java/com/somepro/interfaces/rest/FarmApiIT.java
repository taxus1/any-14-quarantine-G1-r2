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
 * 养殖场档案端到端集成测试（真实 MySQL）。
 *
 * 覆盖：编号 FM-年份-4 位且不重号、字段齐全、默认状态/存栏、改档、停业/注销态、
 * 四条件任意组合查询、空条件全量、翻页不重样且每行带场编号、销档后名册消失、
 * 库里既有存量记录照样翻得出来且不占用新年份序号。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FarmApiIT extends AbstractApiIT {

    private static final String AUTH = "Basic " + Base64.getEncoder()
            .encodeToString("admin:admin123".getBytes(StandardCharsets.UTF_8));

    @Autowired
    private WebTestClient web;

    private final ObjectMapper om = new ObjectMapper();

    private JsonNode postJson(String uri, String json) {
        byte[] body = web.post().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange().expectStatus().isOk().expectBody().returnResult().getResponseBody();
        return parse(body);
    }

    private JsonNode putJson(String uri, String json) {
        byte[] body = web.put().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, AUTH)
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

    private int deleteAndCode(String uri) {
        byte[] body = web.delete().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, AUTH)
                .exchange().expectStatus().isOk().expectBody().returnResult().getResponseBody();
        return parse(body).get("code").asInt();
    }

    private JsonNode parse(byte[] body) {
        try {
            return om.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException(new String(body, StandardCharsets.UTF_8), e);
        }
    }

    private long createFarm(String name, String species, int stock) {
        String body = """
                {"farmName":"%s%s","ownerName":"张三","phone":"13800000000","address":"某村1号",
                 "species":"%s","stockQty":%d}
                """.formatted(TEST_NAME_PREFIX, name, species, stock);
        JsonNode resp = postJson("/api/farms", body);
        assertEquals(0, resp.get("code").asInt(), resp::toString);
        JsonNode d = resp.get("data");
        assertTrue(d.get("farmNo").asText().matches("FM-\\d{4}-\\d{4}"), "编号格式 FM-年份-4位");
        assertEquals("ACTIVE", d.get("status").asText(), "新场默认在用");
        assertEquals(species, d.get("species").asText());
        assertEquals(stock, d.get("stockQty").asInt());
        assertEquals(TEST_NAME_PREFIX + name, d.get("farmName").asText());
        assertEquals("张三", d.get("ownerName").asText());
        return d.get("id").asLong();
    }

    @Test
    @Order(0)
    void legacyRowsRemainReadableAndDoNotOccupyNewYearSequence() {
        // 存量的 2025 场能按编号查到，字段原样（库里先前攒下的记录不能当作没有）
        JsonNode byNo = getJson("/api/farms?farmNo=" + LEGACY_FARM_NO);
        assertEquals(0, byNo.get("code").asInt());
        assertEquals(1, byNo.get("data").get("total").asLong(), "按存量编号精确片段应查到 1 条");
        JsonNode legacy = byNo.get("data").get("content").get(0);
        assertEquals("SHEEP", legacy.get("species").asText());
        assertEquals(88, legacy.get("stockQty").asInt());
        assertEquals("ACTIVE", legacy.get("status").asText());
        // 全量名册里也翻得到
        JsonNode all = getJson("/api/farms?pageNum=1&pageSize=200");
        assertTrue(all.get("data").get("content").findValuesAsText("farmNo").contains(LEGACY_FARM_NO));
    }

    @Test
    @Order(1)
    void farmNoIsUniqueAndSequential() {
        long a = createFarm("顺序号甲场", "PIG", 100);
        long b = createFarm("顺序号乙场", "CATTLE", 50);
        JsonNode fa = getJson("/api/farms/" + a).get("data");
        JsonNode fb = getJson("/api/farms/" + b).get("data");
        String noA = fa.get("farmNo").asText();
        String noB = fb.get("farmNo").asText();
        assertTrue(noA.startsWith("FM-2026-"), "新号落在当前年份");
        assertNotEquals(noA, noB, "同一个号不能落到两家场");
        int seqA = Integer.parseInt(noA.substring(noA.length() - 4));
        int seqB = Integer.parseInt(noB.substring(noB.length() - 4));
        assertEquals(1, seqA, "2025 年存量号不复用、不挤占，新年份从 0001 起");
        assertEquals(seqA + 1, seqB, "年内顺序号递增");
    }

    @Test
    @Order(2)
    void defaultsWhenOptionalFieldsMissing() {
        JsonNode resp = postJson("/api/farms", """
                {"farmName":"IT-只有必填的场","species":"POULTRY"}""");
        assertEquals(0, resp.get("code").asInt(), resp::toString);
        JsonNode d = resp.get("data");
        assertEquals(0, d.get("stockQty").asInt(), "存栏不传默认 0");
        assertEquals("ACTIVE", d.get("status").asText());
        assertTrue(d.get("phone").isNull());
    }

    @Test
    @Order(3)
    void invalidSpeciesRejected() {
        byte[] body = web.post().uri("/api/farms")
                .header(HttpHeaders.AUTHORIZATION, AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"farmName":"IT-非法种类场","species":"DRAGON"}""")
                .exchange().expectStatus().isOk().expectBody().returnResult().getResponseBody();
        assertNotEquals(0, parse(body).get("code").asInt());
    }

    @Test
    @Order(4)
    void modifyProfile() {
        long id = createFarm("待改名场", "SHEEP", 10);
        JsonNode resp = putJson("/api/farms/" + id, """
                {"farmName":"IT-改后名场","phone":"13911112222","stockQty":33}""");
        assertEquals(0, resp.get("code").asInt(), resp::toString);
        JsonNode d = resp.get("data");
        assertEquals("IT-改后名场", d.get("farmName").asText());
        assertEquals("13911112222", d.get("phone").asText());
        assertEquals(33, d.get("stockQty").asInt());
        assertEquals("SHEEP", d.get("species").asText(), "没传的字段不能丢");
        assertEquals("张三", d.get("ownerName").asText(), "没传的字段不能丢");
        assertTrue(d.get("farmNo").asText().startsWith("FM-"));
    }

    @Test
    @Order(5)
    void suspendAndCloseStillInRosterButFilterable() {
        long id = createFarm("停业又注销的场", "PIG", 5);
        JsonNode s = putJson("/api/farms/" + id + "/status", """
                {"status":"SUSPENDED"}""");
        assertEquals("SUSPENDED", s.get("data").get("status").asText());
        JsonNode c = putJson("/api/farms/" + id + "/status", """
                {"status":"CLOSED"}""");
        assertEquals("CLOSED", c.get("data").get("status").asText());
        JsonNode all = getJson("/api/farms?pageNum=1&pageSize=200");
        assertTrue(all.get("data").get("content").findValuesAsText("id")
                .contains(String.valueOf(id)), "注销场仍在名册");
        JsonNode closed = getJson("/api/farms?status=CLOSED&pageSize=200");
        closed.get("data").get("content").forEach(n ->
                assertEquals("CLOSED", n.get("status").asText()));
        assertTrue(closed.get("data").get("content").findValuesAsText("id")
                .contains(String.valueOf(id)));
    }

    @Test
    @Order(6)
    void paginationStableNoOverlapAndEveryRowHasFarmNo() {
        for (int i = 0; i < 6; i++) {
            createFarm("分页专用场%02d".formatted(i), "PIG", i);
        }
        JsonNode p1 = getJson("/api/farms?species=PIG&farmName=IT-&pageNum=1&pageSize=3");
        JsonNode p2 = getJson("/api/farms?species=PIG&farmName=IT-&pageNum=2&pageSize=3");
        assertEquals(0, p1.get("code").asInt());
        var ids1 = p1.get("data").get("content").findValuesAsText("id");
        var ids2 = p2.get("data").get("content").findValuesAsText("id");
        assertEquals(3, ids1.size());
        assertTrue(ids1.stream().noneMatch(ids2::contains), "两页之间不能重样");
        p1.get("data").get("content").forEach(n ->
                assertTrue(n.get("farmNo").asText().matches("FM-\\d{4}-\\d{4}"), "每行都要带场编号"));
    }

    @Test
    @Order(7)
    void noFilterReturnsRosterAndCombinedFilters() {
        JsonNode all = getJson("/api/farms?pageNum=1&pageSize=200");
        assertTrue(all.get("data").get("total").asLong() >= 7, "一个条件不填也得把整份名册调出来（含存量）");

        JsonNode filtered = getJson("/api/farms?species=CATTLE&farmName=IT-&pageNum=1&pageSize=200");
        assertTrue(filtered.get("data").get("total").asLong() >= 1);
        filtered.get("data").get("content").forEach(n ->
                assertEquals("CATTLE", n.get("species").asText()));

        JsonNode byName = getJson("/api/farms?farmName=顺序号甲");
        assertTrue(byName.get("data").get("content").findValuesAsText("farmName")
                .stream().anyMatch(n -> n.contains("顺序号甲场")));
    }

    @Test
    @Order(8)
    void deleteThenMissingFromRoster() {
        long id = createFarm("马上要销的场", "POULTRY", 1);
        assertEquals(0, deleteAndCode("/api/farms/" + id));
        JsonNode detail = getJson("/api/farms/" + id);
        assertNotEquals(0, detail.get("code").asInt(), "销掉后单查也查不到");
        JsonNode page = getJson("/api/farms?pageNum=1&pageSize=200");
        assertFalse(page.get("data").get("content").findValuesAsText("id")
                .contains(String.valueOf(id)), "销掉的不该再从名单里翻出来");
        assertNotEquals(0, deleteAndCode("/api/farms/" + id));
    }
}
