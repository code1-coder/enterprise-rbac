package org.example.rbac.common.result;

import lombok.Data;

import java.util.List;

/**
 * 分页数据，放在 Result.data 里。records 是 UserVO 或 RoleVO，不直接返回实体。
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long size;
    private long current;
    private long pages;
}
