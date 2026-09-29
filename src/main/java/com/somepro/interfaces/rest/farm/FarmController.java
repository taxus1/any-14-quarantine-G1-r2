package com.somepro.interfaces.rest.farm;

import com.somepro.application.farm.FarmAppService;
import com.somepro.application.farm.command.FarmCreateCommand;
import com.somepro.application.farm.command.FarmUpdateCommand;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.farm.converter.FarmVoConverter;
import com.somepro.interfaces.rest.farm.dto.FarmCreateRequest;
import com.somepro.interfaces.rest.farm.dto.FarmStatusRequest;
import com.somepro.interfaces.rest.farm.dto.FarmUpdateRequest;
import com.somepro.interfaces.rest.farm.vo.FarmVO;
import com.somepro.interfaces.rest.common.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 养殖场档案接口（用户接口层）：只做协议适配与 VO 转换，用例编排交给 FarmAppService。
 *
 * - 登记 / 改档 / 改状态（注销、停业）/ 销档 / 单条查 / 名册分页
 * - 名册四个条件（场名、编号、种类、状态）随便拼，都不传返回整份名册
 */
@RestController
@RequestMapping("/api/farms")
public class FarmController {

    private final FarmAppService farmAppService;

    public FarmController(FarmAppService farmAppService) {
        this.farmAppService = farmAppService;
    }

    /** 登记新场：编号自动生成 FM-2026-0001，状态默认 ACTIVE。 */
    @PostMapping
    public Mono<Result<FarmVO>> create(@Valid @RequestBody FarmCreateRequest request) {
        FarmCreateCommand command = new FarmCreateCommand(
                request.farmName(), request.ownerName(), request.phone(),
                request.address(), request.species(), request.stockQty());
        return farmAppService.create(command)
                .map(FarmVoConverter::toVo)
                .map(Result::ok);
    }

    /** 单条查档（台账对号用）。 */
    @GetMapping("/{id}")
    public Mono<Result<FarmVO>> detail(@PathVariable Long id) {
        return farmAppService.detail(id)
                .map(FarmVoConverter::toVo)
                .map(Result::ok);
    }

    /**
     * 名册分页：farmName / farmNo / species / status 任意组合，全空返回全量。
     * pageNum/pageSize 透传，不写死；固定 id 升序，翻页不重样。
     */
    @GetMapping
    public Mono<Result<PageVO<FarmVO>>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String farmName,
            @RequestParam(required = false) String farmNo,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String status) {
        return farmAppService.page(pageNum, pageSize, farmName, farmNo, species, status)
                .map(FarmVoConverter::toPageVo)
                .map(Result::ok);
    }

    /** 改档案：场名、负责人、联系方式、场址、种类、存栏，字段缺省即不改。 */
    @PutMapping("/{id}")
    public Mono<Result<FarmVO>> update(@PathVariable Long id,
                                       @Valid @RequestBody FarmUpdateRequest request) {
        FarmUpdateCommand command = new FarmUpdateCommand(
                request.farmName(), request.ownerName(), request.phone(),
                request.address(), request.species(), request.stockQty());
        return farmAppService.modify(id, command)
                .map(FarmVoConverter::toVo)
                .map(Result::ok);
    }

    /** 改状态：停业 SUSPENDED / 注销 CLOSED / 恢复 ACTIVE（注销是业务状态，仍在名册可筛）。 */
    @PutMapping("/{id}/status")
    public Mono<Result<FarmVO>> changeStatus(@PathVariable Long id,
                                             @Valid @RequestBody FarmStatusRequest request) {
        return farmAppService.changeStatus(id, request.status())
                .map(FarmVoConverter::toVo)
                .map(Result::ok);
    }

    /** 销档：软删除，之后名册里不再出现。 */
    @DeleteMapping("/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id) {
        return farmAppService.delete(id).then(Mono.just(Result.ok()));
    }
}
