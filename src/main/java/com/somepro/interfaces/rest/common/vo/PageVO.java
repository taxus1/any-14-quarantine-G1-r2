package com.somepro.interfaces.rest.common.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 对外分页返回对象（接口层共享 VO）—— 不可变 record。
 *
 * 与 demo 模块自带的 {@code interfaces.rest.demo.vo.PageVO} 同构：
 * 领域层 {@code PageResult} 只有 content/total/pageNum/pageSize 四个组件，
 * 派生的 totalPages 放在接口层暴露，避免把 Jackson 注进领域层。
 */
public record PageVO<T>(List<T> content, long total, int pageNum, int pageSize, int totalPages)
        implements Serializable {
}
