package com.reggie.module.ai.service.impl;

import com.reggie.common.CustomException;
import com.reggie.module.ai.dto.AiAttachmentVO;
import com.reggie.module.ai.mapper.AiAttachmentMapper;
import com.reggie.module.ai.model.AiAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link AiAttachmentServiceImpl} 单元测试：魔数判定真实类型（不信任扩展名）、
 * 大小/空文件校验、附件归属三元组（owner+actorType+tenant）隔离与 base64 data URL。
 *
 * @author reggie
 * @since 2026-09-21
 */
class AiAttachmentServiceImplTest {

    @TempDir
    Path tempDir;

    private AiAttachmentServiceImpl service;
    private AiAttachmentMapper mapper;

    @BeforeEach
    void setUp() {
        service = new AiAttachmentServiceImpl();
        mapper = mock(AiAttachmentMapper.class);
        // ServiceImpl 的 baseMapper 字段：insert 时回填自增主键
        when(mapper.insert(any(AiAttachment.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock inv) {
                AiAttachment a = inv.getArgument(0);
                a.setId(100L);
                return 1;
            }
        });
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "configPath",
                tempDir.toString() + File.separator);
        service.init();
    }

    @Test
    void jpegMagicUploadSucceeds() {
        byte[] bytes = bytesOf((byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, (byte) 0x00);
        AiAttachmentVO vo = service.saveImage(mockFile(bytes, "shell.jsp.png"),
                7L, 900L, "CUSTOMER", "order_assistant");

        // 扩展名是 .png 也必须按魔数识别为 jpeg
        assertEquals("image/jpeg", vo.getMime());
        assertEquals("/api/ai/attachments/100", vo.getUrl());
        // 落盘路径隔离租户与身份
        AiAttachment saved = captureInserted();
        assertTrue(saved.getStoragePath().startsWith("images/ai/7/CUSTOMER/"));
        assertTrue(Files.exists(tempDir.resolve(saved.getStoragePath())));
    }

    @Test
    void pngMagicUploadSucceeds() {
        byte[] bytes = bytesOf((byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
                (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A, (byte) 0x01);
        assertEquals("image/png",
                service.saveImage(mockFile(bytes, "a.png"), 1L, 1L, "MERCHANT", "x").getMime());
    }

    @Test
    void webpMagicUploadSucceeds() {
        byte[] bytes = new byte[16];
        bytes[0] = 0x52; bytes[1] = 0x49; bytes[2] = 0x46; bytes[3] = 0x46;
        bytes[8] = 0x57; bytes[9] = 0x45; bytes[10] = 0x42; bytes[11] = 0x50;
        assertEquals("image/webp",
                service.saveImage(mockFile(bytes, "a.webp"), 1L, 1L, "MERCHANT", "x").getMime());
    }

    @Test
    void fakeImageRejectedByMagic() {
        // 文本内容伪装图片扩展名，必须拒绝
        byte[] bytes = "not an image at all".getBytes();
        CustomException ex = assertThrows(CustomException.class,
                () -> service.saveImage(mockFile(bytes, "evil.png"), 1L, 1L, "MERCHANT", "x"));
        assertTrue(ex.getMessage().contains("仅支持"));
    }

    @Test
    void emptyOrNullFileRejected() {
        MultipartFile empty = mock(MultipartFile.class);
        when(empty.isEmpty()).thenReturn(true);
        assertThrows(CustomException.class,
                () -> service.saveImage(empty, 1L, 1L, "MERCHANT", "x"));
        assertThrows(CustomException.class,
                () -> service.saveImage(null, 1L, 1L, "MERCHANT", "x"));
    }

    @Test
    void missingOwnerRejected() {
        byte[] bytes = bytesOf((byte) 0xFF, (byte) 0xD8, (byte) 0xFF);
        CustomException ex = assertThrows(CustomException.class,
                () -> service.saveImage(mockFile(bytes, "a.jpg"), 1L, null, "MERCHANT", "x"));
        assertTrue(ex.getMessage().contains("登录"));
    }

    @Test
    void oversizedFileRejected() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L * 1024 * 1024 + 1);
        CustomException ex = assertThrows(CustomException.class,
                () -> service.saveImage(file, 1L, 1L, "MERCHANT", "x"));
        assertTrue(ex.getMessage().contains("10MB"));
    }

    @Test
    void listOwnedReturnsOnlyOwnedAttachments() {
        when(mapper.selectById(1L)).thenReturn(attachment(1L, 900L, "CUSTOMER", 7L));
        when(mapper.selectById(2L)).thenReturn(attachment(2L, 901L, "CUSTOMER", 7L)); // 他人
        when(mapper.selectById(3L)).thenReturn(attachment(3L, 900L, "MERCHANT", 7L)); // 身份不符
        when(mapper.selectById(4L)).thenReturn(attachment(4L, 900L, "CUSTOMER", 8L)); // 租户不符
        // 5L 不存在 → null
        List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L, 5L, 1L); // 重复 1L 去重

        List<AiAttachment> result = service.listOwnedByIds(ids, 900L, "CUSTOMER", 7L, 3);
        assertEquals(1, result.size());
        assertEquals(Long.valueOf(1L), result.get(0).getId());
    }

    @Test
    void listOwnedRespectsMaxImages() {
        when(mapper.selectById(1L)).thenReturn(attachment(1L, 900L, "CUSTOMER", 7L));
        when(mapper.selectById(2L)).thenReturn(attachment(2L, 900L, "CUSTOMER", 7L));
        List<AiAttachment> result = service.listOwnedByIds(Arrays.asList(1L, 2L),
                900L, "CUSTOMER", 7L, 1);
        assertEquals(1, result.size());
    }

    @Test
    void mapDataUrlsReturnsBase64ForOwned() throws Exception {
        byte[] bytes = bytesOf((byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0x01);
        AiAttachment att = attachment(1L, 900L, "CUSTOMER", 7L);
        att.setContentType("image/jpeg");
        Files.createDirectories(tempDir.resolve(att.getStoragePath()).getParent());
        Files.write(tempDir.resolve(att.getStoragePath()), bytes);
        when(mapper.selectById(1L)).thenReturn(att);

        Map<Long, String> urls = service.mapDataUrls(Arrays.asList(1L),
                900L, "CUSTOMER", 7L, 3);
        String dataUrl = urls.get(1L);
        assertTrue(dataUrl.startsWith("data:image/jpeg;base64,"));
        // 引用他人图片时不得产出 data URL
        assertTrue(service.mapDataUrls(Arrays.asList(1L), 999L, "CUSTOMER", 7L, 3).isEmpty());
    }

    @Test
    void readMissingFileThrowsCustomException() {
        AiAttachment att = attachment(1L, 900L, "CUSTOMER", 7L);
        att.setStoragePath("images/ai/missing/never.jpg");
        assertThrows(CustomException.class, () -> service.readBytes(att));
    }

    // ==================== 辅助 ====================

    private AiAttachment captureInserted() {
        org.mockito.ArgumentCaptor<AiAttachment> cap =
                org.mockito.ArgumentCaptor.forClass(AiAttachment.class);
        org.mockito.Mockito.verify(mapper).insert(cap.capture());
        return cap.getValue();
    }

    private MultipartFile mockFile(byte[] bytes, String name) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) bytes.length);
        try {
            when(file.getBytes()).thenReturn(bytes);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        when(file.getOriginalFilename()).thenReturn(name);
        return file;
    }

    private AiAttachment attachment(Long id, Long ownerId, String actorType, Long tenantId) {
        AiAttachment a = new AiAttachment();
        a.setId(id);
        a.setOwnerId(ownerId);
        a.setActorType(actorType);
        a.setTenantId(tenantId);
        a.setContentType("image/jpeg");
        a.setStoragePath("images/ai/" + tenantId + "/" + actorType + "/" + id + ".jpg");
        return a;
    }

    private static byte[] bytesOf(byte... head) {
        byte[] b = new byte[head.length + 4];
        System.arraycopy(head, 0, b, 0, head.length);
        return b;
    }
}
