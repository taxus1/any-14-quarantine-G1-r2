package com.somepro.interfaces.rest.eartag;

import com.somepro.application.eartag.EarTagAppService;
import com.somepro.application.eartag.command.EarTagIssueCommand;
import com.somepro.application.eartag.command.EarTagUpdateCommand;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.eartag.converter.EarTagVoConverter;
import com.somepro.interfaces.rest.eartag.dto.EarTagIssueRequest;
import com.somepro.interfaces.rest.eartag.dto.EarTagUpdateRequest;
import com.somepro.interfaces.rest.eartag.vo.EarTagVO;
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
 * 畜禽耳标接口（用户接口层）：只做协议适配与 VO 转换，用例编排交给 EarTagAppService。
 *
 * - 发放（只给场，种类照场走）/ 修改（改挂场、改状态、登记时刻）/ 缴销 / 单条查 / 分页名册
 * - 名册按场、种类、状态翻，耳标号也可查；都不传返回全量
 */
@RestController
@RequestMapping("/api/ear-tags")
public class EarTagController {

    private final EarTagAppService earTagAppService;

    public EarTagController(EarTagAppService earTagAppService) {
        this.earTagAppService = earTagAppService;
    }

    /** 领标：只传 farmId，耳标号自动生成 ET-2026-000001，状态默认 ISSUED。 */
    @PostMapping
    public Mono<Result<EarTagVO>> issue(@Valid @RequestBody EarTagIssueRequest request) {
        EarTagIssueCommand command = new EarTagIssueCommand(request.farmId(), request.issuedAt());
        return earTagAppService.issue(command)
                .map(EarTagVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<EarTagVO>> detail(@PathVariable Long id) {
        return earTagAppService.detail(id)
                .map(EarTagVoConverter::toVo)
                .map(Result::ok);
    }

    /**
     * 耳标名册：farmId / species / status / tagNo 任意组合，全空返回全量。
     * 固定 id 升序，翻页不重样，每行带耳标号。
     */
    @GetMapping
    public Mono<Result<PageVO<EarTagVO>>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tagNo) {
        return earTagAppService.page(pageNum, pageSize, farmId, species, status, tagNo)
                .map(EarTagVoConverter::toPageVo)
                .map(Result::ok);
    }

    /** 改耳标：改挂场（种类跟场走）、改状态（USED 自动记佩戴时刻）、补记发放/佩戴时刻。 */
    @PutMapping("/{id}")
    public Mono<Result<EarTagVO>> update(@PathVariable Long id,
                                         @Valid @RequestBody EarTagUpdateRequest request) {
        EarTagUpdateCommand command = new EarTagUpdateCommand(
                request.farmId(), request.status(), request.issuedAt(), request.wornAt());
        return earTagAppService.modify(id, command)
                .map(EarTagVoConverter::toVo)
                .map(Result::ok);
    }

    /** 缴销：软删除，之后名册里不再出现。 */
    @DeleteMapping("/{id}")
    public Mono<Result<EarTagVO>> deleteResult(@PathVariable Long id) {
        return earTagAppService.delete(id).then(Mono.just(Result.ok()));
    }
}
