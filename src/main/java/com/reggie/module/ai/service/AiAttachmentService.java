package com.reggie.module.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.ai.dto.AiAttachmentVO;
import com.reggie.module.ai.model.AiAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * AI 聊天图片附件服务：上传校验落盘、归属读取、模型 base64 装配。
 *
 * @author reggie
 * @since 2026-09-20
 */
public interface AiAttachmentService extends IService<AiAttachment> {

    /** 单条消息 / 单次请求最多装配图片张数（控制 token 与上游体积） */
    int MAX_IMAGES_PER_REQUEST = 3;


    /**
     * 保存聊天图片：魔数校验 jpg/png/webp、限 10MB，落盘到 images/ai/... 并落库。
     *
     * @param file      上传文件
     * @param tenantId  租户ID
     * @param ownerId   上传者ID
     * @param actorType 上传者身份 EMPLOYEE/CUSTOMER
     * @param scene     上传场景（可空）
     * @return 附件 VO（含鉴权读取 URL）
     */
    AiAttachmentVO saveImage(MultipartFile file, Long tenantId, Long ownerId, String actorType, String scene);

    /**
     * 按主键取未删除附件，不存在返回 null。
     */
    AiAttachment getById(Long id);

    /**
     * 读取附件字节。
     */
    byte[] readBytes(AiAttachment attachment);

    /**
     * 归属校验后列出附件实体（保序、按首次出现去重、限量），不存在/无权一律跳过。
     * <p>是元数据视图与 data URL 装配的共同基础。</p>
     */
    List<AiAttachment> listOwnedByIds(List<Long> ids, Long ownerId, String actorType,
                                      Long tenantId, int maxImages);

    /**
     * 归属校验后的附件元数据视图（落 ai_message.attachments JSON，权威字段防前端伪造）。
     */
    List<AiAttachmentVO> describeViews(List<Long> ids, Long ownerId, String actorType,
                                       Long tenantId, int maxImages);

    /**
     * 装配模型入参：attachmentId → data URL（data:image/...;base64,xxx）。
     * <p>读图 + base64；不存在/无权/坏文件跳过。key 与有效附件 ID 一致。</p>
     */
    Map<Long, String> mapDataUrls(List<Long> ids, Long ownerId, String actorType,
                                  Long tenantId, int maxImages);

    /** 实体转视图（统一 URL 规则） */
    AiAttachmentVO toView(AiAttachment attachment);
}
