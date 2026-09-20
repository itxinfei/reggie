package com.reggie.module.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.module.ai.dto.AiAttachmentVO;
import com.reggie.module.ai.mapper.AiAttachmentMapper;
import com.reggie.module.ai.model.AiAttachment;
import com.reggie.module.ai.service.AiAttachmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * AI 图片附件服务实现。
 * <p>存储根目录复用 {@code reggie.path}（与通用上传一致），AI 图片独立子目录 images/ai/。
 * 模型入参统一转 base64 data URL，不向第三方暴露本站附件 URL。</p>
 *
 * @author reggie
 * @since 2026-09-20
 */
@Slf4j
@Service
public class AiAttachmentServiceImpl extends ServiceImpl<AiAttachmentMapper, AiAttachment>
        implements AiAttachmentService {

    /** 单图上限 10MB（与 application.yml multipart max-file-size 对齐） */
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46}; // "RIFF"
    private static final byte[] WEBP_MAGIC = {0x57, 0x45, 0x42, 0x50}; // "WEBP"

    @Value("${reggie.path:}")
    private String configPath;

    private String basePath;

    /**
     * 初始化上传根目录：配置了 reggie.path 用配置，否则 jar/工作目录下 uploads。
     */
    @PostConstruct
    public void init() {
        if (configPath != null && !configPath.isEmpty()) {
            basePath = configPath;
        } else {
            String userDir = System.getProperty("user.dir");
            if (userDir.contains("target") && userDir.endsWith("classes")) {
                userDir = new File(userDir).getParentFile().getParent();
            }
            basePath = new File(userDir, "uploads").getAbsolutePath() + File.separator;
        }
        File dir = new File(basePath);
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("AI附件目录创建失败: {}", basePath);
        }
    }

    @Override
    public AiAttachmentVO saveImage(MultipartFile file, Long tenantId, Long ownerId,
                                    String actorType, String scene) {
        if (file == null || file.isEmpty()) {
            throw new CustomException("上传图片不能为空");
        }
        if (ownerId == null) {
            throw new CustomException("登录状态已过期，请重新登录");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException("图片大小不能超过10MB");
        }

        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传图片失败", e);
            throw new CustomException("图片读取失败，请重试");
        }
        if (bytes.length == 0 || bytes.length > MAX_FILE_SIZE) {
            throw new CustomException("图片大小不能超过10MB");
        }

        // 魔数判定真实类型，不信任扩展名 / Content-Type（防 shell.jsp.png 绕过）
        String mime = detectMime(bytes);
        if (mime == null) {
            throw new CustomException("仅支持 JPG、PNG、WebP 格式图片");
        }
        String ext = mimeExtension(mime);

        Integer width = null;
        Integer height = null;
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image != null) {
                width = image.getWidth();
                height = image.getHeight();
            }
        } catch (Exception e) {
            // JDK ImageIO 不支持 webp 时返回 null，宽高留空不阻断上传
            log.debug("读取图片尺寸失败（webp 可能不支持）: mime={}", mime);
        }

        String sha256 = sha256Hex(bytes);
        String tenantDir = tenantId != null ? tenantId.toString() : "0";
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String storagePath = "images/ai/" + tenantDir + "/" + actorType + "/" + fileName;

        File dest = new File(basePath + storagePath);
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new CustomException("存储目录创建失败");
        }
        try {
            Files.write(dest.toPath(), bytes);
        } catch (IOException e) {
            log.error("AI图片落盘失败: path={}", storagePath, e);
            throw new CustomException("图片保存失败，请重试");
        }

        AiAttachment record = new AiAttachment();
        record.setTenantId(tenantId);
        record.setActorType(actorType);
        record.setOwnerId(ownerId);
        record.setScene(truncate(scene, 50));
        record.setFileName(fileName);
        record.setOriginalName(truncate(file.getOriginalFilename(), 200));
        record.setContentType(mime);
        record.setFileSize((long) bytes.length);
        record.setWidth(width);
        record.setHeight(height);
        record.setSha256(sha256);
        record.setStoragePath(storagePath);
        record.setIsDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        record.setCreateTime(now);
        record.setUpdateTime(now);
        this.save(record);

        log.info("AI图片上传成功: id={}, owner={}/{}, mime={}, size={}, {}x{}, path={}",
                record.getId(), actorType, ownerId, mime, bytes.length, width, height, storagePath);

        AiAttachmentVO vo = new AiAttachmentVO();
        vo.setAttachmentId(record.getId());
        vo.setType("image");
        vo.setMime(mime);
        vo.setUrl("/api/ai/attachments/" + record.getId());
        vo.setWidth(width);
        vo.setHeight(height);
        vo.setFileSize((long) bytes.length);
        return vo;
    }

    @Override
    public AiAttachment getById(Long id) {
        if (id == null) {
            return null;
        }
        return super.getById(id);
    }

    @Override
    public byte[] readBytes(AiAttachment attachment) {
        try {
            return Files.readAllBytes(Paths.get(basePath + attachment.getStoragePath()));
        } catch (IOException e) {
            log.error("读取AI附件失败: id={}, path={}", attachment.getId(), attachment.getStoragePath(), e);
            throw new CustomException("图片读取失败");
        }
    }

    @Override
    public List<AiAttachment> listOwnedByIds(List<Long> ids, Long ownerId, String actorType,
                                             Long tenantId, int maxImages) {
        List<AiAttachment> result = new ArrayList<>();
        if (ids == null || ids.isEmpty() || ownerId == null) {
            return result;
        }
        int limit = Math.max(1, maxImages);
        Set<Long> seen = new HashSet<>();
        for (Long id : ids) {
            if (id == null || !seen.add(id)) {
                continue;
            }
            if (result.size() >= limit) {
                break;
            }
            AiAttachment att = super.getById(id);
            // 模型入参严格 owner 校验（身份+ID，租户再叠加），杜绝引用他人图片
            if (!isOwned(att, ownerId, actorType, tenantId)) {
                log.warn("AI附件归属校验未通过，跳过: id={}, requester={}/{}, tenant={}",
                        id, actorType, ownerId, tenantId);
                continue;
            }
            result.add(att);
        }
        return result;
    }

    @Override
    public List<AiAttachmentVO> describeViews(List<Long> ids, Long ownerId, String actorType,
                                               Long tenantId, int maxImages) {
        List<AiAttachmentVO> views = new ArrayList<>();
        for (AiAttachment att : listOwnedByIds(ids, ownerId, actorType, tenantId, maxImages)) {
            views.add(toView(att));
        }
        return views;
    }

    @Override
    public Map<Long, String> mapDataUrls(List<Long> ids, Long ownerId, String actorType,
                                         Long tenantId, int maxImages) {
        Map<Long, String> result = new LinkedHashMap<>();
        for (AiAttachment att : listOwnedByIds(ids, ownerId, actorType, tenantId, maxImages)) {
            try {
                byte[] bytes = readBytes(att);
                String b64 = Base64.getEncoder().encodeToString(bytes);
                result.put(att.getId(), "data:" + att.getContentType() + ";base64," + b64);
            } catch (Exception e) {
                log.warn("AI附件转base64失败，跳过: id={}", att.getId(), e);
            }
        }
        return result;
    }

    @Override
    public AiAttachmentVO toView(AiAttachment att) {
        if (att == null) {
            return null;
        }
        AiAttachmentVO vo = new AiAttachmentVO();
        vo.setAttachmentId(att.getId());
        vo.setType("image");
        vo.setMime(att.getContentType());
        vo.setUrl("/api/ai/attachments/" + att.getId());
        vo.setWidth(att.getWidth());
        vo.setHeight(att.getHeight());
        vo.setFileSize(att.getFileSize());
        return vo;
    }

    /** 归属判定：附件存在 + owner/身份一致 +（附件有租户时）租户一致 */
    private boolean isOwned(AiAttachment att, Long ownerId, String actorType, Long tenantId) {
        return att != null
                && ownerId.equals(att.getOwnerId())
                && actorType != null && actorType.equals(att.getActorType())
                && (att.getTenantId() == null || att.getTenantId().equals(tenantId));
    }

    // ==================== 私有辅助 ====================

    /**
     * 魔数检测真实图片类型。
     *
     * @return image/jpeg、image/png、image/webp；不识别返回 null
     */
    private String detectMime(byte[] b) {
        if (startsWith(b, JPEG_MAGIC)) {
            return "image/jpeg";
        }
        if (startsWith(b, PNG_MAGIC)) {
            return "image/png";
        }
        // WebP：前4字节 "RIFF"，偏移 8 的 4 字节 "WEBP"
        if (b.length >= 12 && startsWith(b, RIFF_MAGIC)
                && b[8] == WEBP_MAGIC[0] && b[9] == WEBP_MAGIC[1]
                && b[10] == WEBP_MAGIC[2] && b[11] == WEBP_MAGIC[3]) {
            return "image/webp";
        }
        return null;
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String mimeExtension(String mime) {
        if ("image/png".equals(mime)) {
            return "png";
        }
        if ("image/webp".equals(mime)) {
            return "webp";
        }
        return "jpg";
    }

    private String sha256Hex(byte[] bytes) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte h : hash) {
                String hex = Integer.toHexString(0xff & h);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            // SHA-256 是 JDK 内置算法，理论不会缺失
            throw new CustomException("文件摘要计算失败");
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
