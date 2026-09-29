package com.somepro.interfaces.rest.shared;

import java.io.Serializable;
import java.util.List;

/**
 * 对外分页返回对象（VO，用户接口层）—— 不可变 record，所有业务模块共用。
 *
 * 与领域层 {@code PageResult} 的分工：PageResult 只有 content/total/pageNum/pageSize，
 * 保持领域层零框架依赖；totalPages 是给前端渲染分页器的派生字段，放在接口层。
 */
public record PageVO<T>(List<T> content, long total, int pageNum, int pageSize, int totalPages)
        implements Serializable {
}
