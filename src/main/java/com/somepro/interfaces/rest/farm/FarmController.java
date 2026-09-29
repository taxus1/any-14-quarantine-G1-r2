package com.somepro.interfaces.rest.farm;

import com.somepro.application.farm.FarmAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.farm.converter.FarmVoConverter;
import com.somepro.interfaces.rest.farm.vo.FarmVO;
import com.somepro.interfaces.rest.shared.PageVO;
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
 * 养殖场档案接口：登记 / 修改 / 查明细 / 翻名册 / 注销。
 *
 * 路由约定（与耳标模块一致）：
 * - POST /api/farms            登记
 * - POST /api/farms/{id}       修改（字段级部分更新）
 * - GET  /api/farms/{id}       明细
 * - GET  /api/farms            名册（条件任意组合，可全空；分页透传）
 * - POST /api/farms/{id}/close 注销（软删除，之后名册里翻不出来）
 */
@RestController
@RequestMapping("/api/farms")
public class FarmController {

    private final FarmAppService farmAppService;

    public FarmController(FarmAppService farmAppService) {
        this.farmAppService = farmAppService;
    }

    /** 登记养殖场。编号自动生成（FM-2026-0001），状态默认 ACTIVE，存栏默认 0。 */
    @PostMapping
    public Mono<Result<FarmVO>> create(@RequestParam String farmName,
                                       @RequestParam(required = false) String ownerName,
                                       @RequestParam(required = false) String phone,
                                       @RequestParam(required = false) String address,
                                       @RequestParam String species,
                                       @RequestParam(required = false) Integer stockQty) {
        return farmAppService.register(farmName, ownerName, phone, address, species, stockQty)
                .map(FarmVoConverter::toVo)
                .map(Result::ok);
    }

    /** 修改档案：不传的字段保持原值，传空串清空可空字段，场编号不可改。 */
    @PostMapping("/{id}")
    public Mono<Result<FarmVO>> update(@PathVariable Long id, ServerWebExchange exchange) {
        return farmAppService.edit(id,
                        QueryParams.change(exchange, "farmName"),
                        QueryParams.change(exchange, "ownerName"),
                        QueryParams.change(exchange, "phone"),
                        QueryParams.change(exchange, "address"),
                        QueryParams.change(exchange, "species"),
                        QueryParams.changeInt(exchange, "stockQty"),
                        QueryParams.change(exchange, "status"))
                .map(FarmVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<FarmVO>> detail(@PathVariable Long id) {
        return farmAppService.detail(id)
                .map(FarmVoConverter::toVo)
                .map(Result::ok);
    }

    /**
     * 翻名册。farmName / farmNo / species / status 四个条件随便拼，一个都不填返回整份名册；
     * 按 id 升序逐页往下走，两页之间不重样。
     */
    @GetMapping
    public Mono<Result<PageVO<FarmVO>>> page(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "20") int pageSize,
                                             @RequestParam(required = false) String farmName,
                                             @RequestParam(required = false) String farmNo,
                                             @RequestParam(required = false) String species,
                                             @RequestParam(required = false) String status) {
        return farmAppService.page(pageNum, pageSize, farmName, farmNo, species, status)
                .map(FarmVoConverter::toPageVo)
                .map(Result::ok);
    }

    /** 注销：销掉的档案不再从名单里翻出来。 */
    @PostMapping("/{id}/close")
    public Mono<Result<Void>> close(@PathVariable Long id) {
        return farmAppService.close(id).then(Mono.just(Result.ok()));
    }
}
