package com.reggie.module.ai.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.RateLimitType;
import com.reggie.module.ai.dto.AiAttachmentVO;
import com.reggie.module.ai.model.AiAttachment;
import com.reggie.module.ai.model.AiChatConstants;
import com.reggie.module.ai.service.AiAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Objects;

/**
 * AI 聊天图片附件：上传（魔数+限流+归属落库）、鉴权读取。
 *
 * @author reggie
 * @since 2026-09-20
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/attachments")
@Tag(name = "AI图片附件", description = "AI聊天图片上传与鉴权读取（视觉多模态）")
public class AiAttachmentController {

    @Resource
    private AiAttachmentService aiAttachmentService;

    @PostMapping("/image")
    @RateLimit(maxRequestsPerSecond = 5, type = RateLimitType.USER)
    @Operation(summary = "上传聊天图片", description = "仅 JPG/PNG/WebP，最大10MB，魔数校验")
    public R<AiAttachmentVO> uploadImage(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "scene", required = false) String scene,
                                         HttpServletRequest httpRequest) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return R.error("NOTLOGIN");
        }
        String actorType = resolveActorType(httpRequest);
        AiAttachmentVO vo = aiAttachmentService.saveImage(
                file, BaseContext.getCurrentTenantId(), userId, actorType, scene);
        return R.success(vo);
    }

    @GetMapping("/{id}")
    @RateLimit(maxRequestsPerSecond = 10, type = RateLimitType.USER)
    @Operation(summary = "读取聊天图片", description = "仅所有者或同租户员工可读")
    public ResponseEntity<byte[]> download(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        String actorType = resolveActorType(httpRequest);
        Long tenantId = BaseContext.getCurrentTenantId();

        AiAttachment att = aiAttachmentService.getById(id);
        if (att == null) {
            return ResponseEntity.notFound().build();
        }
        boolean isOwner = userId.equals(att.getOwnerId()) && actorType.equals(att.getActorType());
        boolean sameTenantEmployee = AiChatConstants.ACTOR_EMPLOYEE.equals(actorType)
                && att.getTenantId() != null && Objects.equals(att.getTenantId(), tenantId);
        if (!isOwner && !sameTenantEmployee) {
            log.warn("AI附件越权读取被拒: id={}, requester={}/{}, owner={}/{}",
                    id, actorType, userId, att.getActorType(), att.getOwnerId());
            return ResponseEntity.status(403).build();
        }

        byte[] bytes = aiAttachmentService.readBytes(att);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(att.getContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=31536000, immutable")
                .header("X-Content-Type-Options", "nosniff")
                .body(bytes);
    }

    /**
     * 与 {@link AIChatController} 一致的身份消歧：session 含 employee=员工，否则按 C 端用户。
     */
    private String resolveActorType(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null && session.getAttribute("employee") != null) {
            return AiChatConstants.ACTOR_EMPLOYEE;
        }
        return AiChatConstants.ACTOR_CUSTOMER;
    }
}
