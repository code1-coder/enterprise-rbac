package org.example.rbac.common.exception;

/**
 * 骨架占位异常。ServiceImpl 经 SkeletonSupport.pending 抛出，表示对应用例还没写。
 */
public final class Unimplemented {

    private Unimplemented() {
    }

    public static BusinessException of(String action) {
        return new BusinessException(500, action + "尚未实现");
    }
}
