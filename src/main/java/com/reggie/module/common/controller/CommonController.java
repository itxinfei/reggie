package com.reggie.module.common.controller;

import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 * 公共文件控制器
 * 提供文件上传和下载接口
 * </p>
 *
 * @author reggie
 * @since 2026-07-09
 */
@RestController
@RequestMapping("/common")
@Slf4j
@RequireEmployee
@Tag(name = "公共接口", description = "文件上传下载等公共接口")
public class CommonController {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final int BUFFER_SIZE = 1024;

    /** 图片魔数：扩展名 → 合法文件头集合 */
    private static final Map<String, byte[][]> MAGIC_BYTES = new HashMap<>();
    static {
        MAGIC_BYTES.put("jpg", new byte[][]{
                { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }
        });
        MAGIC_BYTES.put("jpeg", new byte[][]{
                { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }
        });
        MAGIC_BYTES.put("png", new byte[][]{
                { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A }
        });
        MAGIC_BYTES.put("gif", new byte[][]{
                { 0x47, 0x49, 0x46, 0x38, 0x37, 0x61 },
                { 0x47, 0x49, 0x46, 0x38, 0x39, 0x61 }
        });
    }

    @Value("${reggie.path:}")
    private String configPath;

    private String basePath;

    /**
     * 初始化上传目录：jar包所在目录下的 uploads 文件夹
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
        if (!dir.exists()) {
            dir.mkdirs();
        }

        log.info("文件上传目录初始化完成: {}", basePath);
    }

    /**
     * 检查登录态（文件上传/下载需要登录）
     */
    private R<String> checkLogin(HttpServletRequest request) {
        if (request.getSession().getAttribute("employee") == null
                && request.getSession().getAttribute("user") == null) {
            return R.error("NOTLOGIN");
        }
        return null; // 已登录
    }

    /**
     * 文件上传
     * @param file 上传的文件
     * @return 上传后的文件路径
     */
    @PostMapping("/upload")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "文件上传", description = "上传图片文件（支持jpg、jpeg、png、gif，最大5MB），需要登录")
    @Parameter(name = "file", description = "上传的文件", required = true)
    public R<String> upload(MultipartFile file, HttpServletRequest request) {
        // 登录态校验
        R<String> loginCheck = checkLogin(request);
        if (loginCheck != null) {
            return loginCheck;
        }
        // 1. 校验文件是否为空
        if (file.isEmpty()) {
            return R.error("上传文件不能为空");
        }

        // 2. 校验文件类型（仅允许图片格式）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return R.error("文件名不合法");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return R.error("文件类型不支持，仅支持jpg、jpeg、png、gif格式");
        }

        // 3. 校验文件大小（5MB）
        if (file.getSize() > MAX_FILE_SIZE) {
            return R.error("文件大小不能超过5MB");
        }

        // 4. 校验文件魔数（Magic Bytes），防止扩展名绕过（如 shell.jsp.png）
        if (!checkMagicBytes(file, extension)) {
            return R.error("文件内容与声明类型不符，上传被拒绝");
        }

        //file是一个临时文件，需要转存到指定位置，否则本次请求完成后临时文件会删除
        log.info("文件上传：originalFilename={}, size={}", originalFilename, file.getSize());

        //原始文件名
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        //使用UUID重新生成文件名，防止文件名称重复造成文件覆盖
        String fileName = UUID.randomUUID().toString() + suffix;

        // 使用 UUID 生成文件名，保存到 images/dishes/ 子目录
        String subDir = "images/dishes/";
        String relativePath = subDir + fileName;

        // 打印调试信息
        log.info("文件上传: originalFilename={}, size={} bytes, path={}", originalFilename, file.getSize(), basePath + relativePath);
        File dir = new File(basePath + subDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            file.transferTo(new File(basePath + relativePath));
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return R.error("文件上传失败");
        }
        return R.success(relativePath);
    }

    /**
     * 校验文件魔数（Magic Bytes），确认文件内容与声明的扩展名一致
     * <p>
     * 仅校验扩展名可被绕过（如将 shell.jsp 重命名为 .png），魔数校验可拒绝此类伪装。
     *
     * @param file      MultipartFile
     * @param extension 小写扩展名
     * @return true=魔数匹配
     */
    private boolean checkMagicBytes(MultipartFile file, String extension) {
        byte[][] expectedMags = MAGIC_BYTES.get(extension);
        if (expectedMags == null || expectedMags.length == 0) {
            return false;
        }
        InputStream is = null;
        try {
            is = file.getInputStream();
            int maxLen = expectedMags[0].length;
            for (byte[] other : expectedMags) {
                if (other.length > maxLen) {
                    maxLen = other.length;
                }
            }
            byte[] head = new byte[maxLen];
            int bytesRead = is.read(head);
            if (bytesRead < maxLen) {
                return false;
            }
            for (byte[] expected : expectedMags) {
                boolean match = true;
                for (int i = 0; i < expected.length; i++) {
                    if (head[i] != expected[i]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            log.error("文件魔数校验失败: extension={}, error={}", extension, e.getMessage(), e);
            return false;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.warn("文件流关闭异常", e);
                }
            }
        }
    }

    /**
     * 文件下载
     * @param name 文件名（支持 / 或 \ 分隔符）
     * @param response HTTP响应对象
     */
    @GetMapping("/download")
    @Operation(summary = "文件下载", description = "下载图片文件，需要登录")
    @Parameter(name = "name", description = "文件名", required = true)
    public void download(String name, HttpServletResponse response, HttpServletRequest request) {
        // 登录态校验
        if (request.getSession().getAttribute("employee") == null
                && request.getSession().getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":0,\"msg\":\"NOTLOGIN\"}");
            } catch (IOException e) {
                log.error("发送登录校验响应失败", e);
            }
            return;
        }
        String filePath = null;
        try {
            // 先解码 URL 编码字符（浏览器会自动对反斜杠等字符编码）
            String decodedName = java.net.URLDecoder.decode(name, java.nio.charset.StandardCharsets.UTF_8.name());
            // 统一分隔符为 /
            String normalizedPath = decodedName.replace("\\", "/");
            File baseDir = new File(basePath).getCanonicalFile();
            File targetFile = new File(baseDir, normalizedPath).getCanonicalFile();

            // 校验目标路径是否在允许的基础路径内
            if (!targetFile.getPath().startsWith(baseDir.getPath())) {
                log.warn("路径穿越攻击被拦截: name={}, resolved={}", name, targetFile.getPath());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "非法路径访问");
                return;
            }

            filePath = targetFile.getAbsolutePath();

            log.info("文件下载请求: name={}, path={}, exists={}", name, filePath, targetFile.exists());

            // 如果文件不存在，返回 SVG 占位图（不依赖外部文件）
            if (!targetFile.exists()) {
                log.warn("文件不存在，返回占位图: {}", filePath);
                sendPlaceholderImage(response);
                return;
            }

            // 根据文件扩展名设置Content-Type（使用解码后的文件名提取扩展名，避免 URL 编码干扰）
            int dotIdx = decodedName.lastIndexOf(".");
            if (dotIdx < 0 || dotIdx == decodedName.length() - 1) {
                // 无扩展名或以点号结尾，按二进制流处理
                response.setContentType("application/octet-stream");
            } else {
                String extension = decodedName.substring(dotIdx + 1).toLowerCase();
                switch (extension) {
                    case "jpg":
                    case "jpeg":
                        response.setContentType("image/jpeg");
                        break;
                    case "png":
                        response.setContentType("image/png");
                        break;
                    case "gif":
                        response.setContentType("image/gif");
                        break;
                    default:
                        response.setContentType("application/octet-stream");
                }
            }

            try (FileInputStream fileInputStream = new FileInputStream(targetFile);
                 ServletOutputStream outputStream = response.getOutputStream()) {
                int len;
                byte[] bytes = new byte[BUFFER_SIZE];
                while ((len = fileInputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, len);
                    outputStream.flush();
                }
            }
        } catch (Exception e) {
            log.error("文件下载失败: {}", filePath, e);
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            } catch (IOException ex) {
                log.error("发送错误响应失败", ex);
            }
        }
    }

    /**
     * 返回 SVG 占位图（图片不存在时显示）
     */
    private void sendPlaceholderImage(HttpServletResponse response) throws IOException {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"200\" viewBox=\"0 0 200 200\">" +
                "<rect width=\"200\" height=\"200\" fill=\"#f0f0f0\"/>" +
                "<text x=\"100\" y=\"90\" font-family=\"Arial\" font-size=\"14\" fill=\"#999\" text-anchor=\"middle\">No Image</text>" +
                "<text x=\"100\" y=\"115\" font-family=\"Arial\" font-size=\"12\" fill=\"#bbb\" text-anchor=\"middle\">&#x1F5BC;</text>" +
                "</svg>";
        response.setContentType("image/svg+xml");
        response.getWriter().write(svg);
        response.getWriter().flush();
    }
}

