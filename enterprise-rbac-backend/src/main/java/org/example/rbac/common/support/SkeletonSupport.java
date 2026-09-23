package org.example.rbac.common.support;

import org.example.rbac.common.exception.BusinessException;
import org.example.rbac.common.exception.Unimplemented;

import java.util.Objects;

/**
 * 骨架共用出口：先确认该方法点名的 Mapper 已注入，再抛出未实现异常。
 * 只检查 Controller 到 Mapper 的装配是否还在，不访问数据库。
 */
public final class SkeletonSupport {

    private SkeletonSupport() {
    }

    public static BusinessException pending(String action, Object... dependencies) {
        for (Object dependency : dependencies) {
            Objects.requireNonNull(dependency, "骨架依赖未注入");
        }
        return Unimplemented.of(action);
    }
}
