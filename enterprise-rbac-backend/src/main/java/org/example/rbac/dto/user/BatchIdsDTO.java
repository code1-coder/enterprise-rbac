package org.example.rbac.dto.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * DELETE /api/users/batch 的入参，按这些 id 逻辑删除 sys_user，并处理 sys_user_role。
 */
@Data
public class BatchIdsDTO {

    @NotEmpty(message = "请选择要删除的用户")
    private List<@NotNull(message = "用户 ID 不能为空") Long> ids;
}
