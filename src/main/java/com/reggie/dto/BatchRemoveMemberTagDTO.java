package com.reggie.dto;

import lombok.Data;
import java.util.List;

/**
 * 批量删除会员标签请求DTO
 *
 * @author reggie
 * @since 2026-07-10
 */
@Data
public class BatchRemoveMemberTagDTO {

    /** 标签ID列表 */
    private List<Long> tagIds;
}
