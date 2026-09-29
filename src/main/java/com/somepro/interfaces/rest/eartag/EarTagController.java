package com.somepro.interfaces.rest.eartag;

import com.somepro.application.eartag.EarTagAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.eartag.converter.EarTagVoConverter;
import com.somepro.interfaces.rest.eartag.vo.EarTagVO;
import com.somepro.interfaces.rest.shared.PageVO;
import com.somepro.interfaces.rest.support.DateTimeParams;
import com.somepro.interfaces.rest.support.QueryParams;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 畜禽耳标接口：发放（领用）/ 修改 / 查明细 / 翻名册 / 注销。
 *
 * 路由约定：
 * - POST /api/ear-tags            发放（只传 farmId，种类自动随场；状态默认 ISSUED）
 * - POST /api/ear-tags/{id}       修改（改挂场时传新 farmId，种类仍随新场）
 * - GET  /api/ear-tags/{id}       明细
 * - GET  /api/ear-tags            名册（按场/种类/状态任意组合，可全空；每行带耳标号与场编号）
 * - POST /api/ear-tags/{id}/close 注销（软删除）
 */
@RestController
@RequestMapping("/api/ear-tags")
public class EarTagController {

    private final EarTagAppService earTagAppService;

    public EarTagController(EarTagAppService earTagAppService) {
        this.earTagAppService = earTagAppService;
    }

    /**
     * 发放（领用）耳标。不接收种类参数 —— 挂到哪家场就照那家场的种类走。
     */
    @PostMapping
    public Mono<Result<EarTagVO>> create(@RequestParam Long farmId,
                                         @RequestParam(required = false) String issuedAt) {
        return earTagAppService.issue(farmId, DateTimeParams.parse(issuedAt))
                .map(EarTagVoConverter::toVo)
                .map(Result::ok);
    }

    /** 修改耳标：改挂场传新 farmId（种类由新场带出）；时间传空串清空；置 USED 不传 wornAt 自动记当下。 */
    @PostMapping("/{id}")
    public Mono<Result<EarTagVO>> update(@PathVariable Long id, ServerWebExchange exchange) {
        return earTagAppService.edit(id,
                        QueryParams.changeLong(exchange, "farmId"),
                        QueryParams.changeDateTime(exchange, "issuedAt"),
                        QueryParams.changeDateTime(exchange, "wornAt"),
                        QueryParams.change(exchange, "status"))
                .map(EarTagVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<EarTagVO>> detail(@PathVariable Long id) {
        return earTagAppService.detail(id)
                .map(EarTagVoConverter::toVo)
                .map(Result::ok);
    }

    /** 翻名册：farmId / species / status 随便拼，全不填返回全部；按 id 升序逐页翻，不重样。 */
    @GetMapping
    public Mono<Result<PageVO<EarTagVO>>> page(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "20") int pageSize,
                                               @RequestParam(required = false) Long farmId,
                                               @RequestParam(required = false) String species,
                                               @RequestParam(required = false) String status) {
        return earTagAppService.page(pageNum, pageSize, farmId, species, status)
                .map(EarTagVoConverter::toPageVo)
                .map(Result::ok);
    }

    /** 注销：销掉的耳标不再从名单里翻出来。 */
    @PostMapping("/{id}/close")
    public Mono<Result<Void>> close(@PathVariable Long id) {
        return earTagAppService.disable(id).then(Mono.just(Result.ok()));
    }
}
