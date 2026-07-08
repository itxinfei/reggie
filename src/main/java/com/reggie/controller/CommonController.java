package com.reggie.controller;

import com.reggie.common.R;
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
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传和下载
 */
@RestController
@RequestMapping("/common")
@Slf4j
@Tag(name = "公共接口", description = "文件上传下载等公共接口")
public class CommonController {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final int BUFFER_SIZE = 1024;

    @Value("${reggie.path:}")
    private String configPath;

    private String basePath;

    /**
     * 初始化上传目录：jar包所在目录下的 uploads 文件夹
     */
    @PostConstruct
    public void init() {
        if (configPath != null && !configPath.isEmpty()) {
            // 使用配置文件指定的路径
            basePath = configPath;
        } else {
            // 优先使用项目根目录（兼容开发和部署环境）
            String userDir = System.getProperty("user.dir");

            // 检查是否在 target/classes 目录下运行（IDE 开发模式）
            if (userDir.contains("target") && userDir.endsWith("classes")) {
                // 回退到项目根目录
                userDir = new File(userDir).getParentFile().getParent();
            }

            basePath = new File(userDir, "uploads").getAbsolutePath() + File.separator;
        }

        // 确保目录存在
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        log.info("文件上传目录: {}", basePath);
    }

    /**
     * 文件上传
     *
     * @param file
     * @return 文件上传的目录改为项目运行的根目录
     */
    @PostMapping("/upload")
    @Operation(summary = "文件上传", description = "上传图片文件（支持jpg、jpeg、png、gif，最大5MB）")
    @Parameter(name = "file", description = "上传的文件", required = true)
    public R<String> upload(MultipartFile file) {
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

        //file是一个临时文件，需要转存到指定位置，否则本次请求完成后临时文件会删除
        log.info("文件上传：originalFilename={}, size={}", originalFilename, file.getSize());

        //原始文件名
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        //使用UUID重新生成文件名，防止文件名称重复造成文件覆盖
        String fileName = UUID.randomUUID().toString() + suffix;

        // 保存到 images/dishes/ 子目录，与数据库中的路径格式一致
        String subDir = "images" + File.separator + "dishes" + File.separator;

        // 打印调试信息
        log.info("文件上传: originalFilename={}, size={} bytes, path={}", originalFilename, file.getSize(), basePath + subDir + fileName);
        File dir = new File(basePath + subDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            file.transferTo(new File(basePath + subDir + fileName));
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return R.error("文件上传失败");
        }
        return R.success(subDir + fileName);
    }

    /**
     * 文件下载
     *
     * @param name
     * @param response
     */
    @GetMapping("/download")
    @Operation(summary = "文件下载", description = "下载图片文件")
    @Parameter(name = "name", description = "文件名", required = true)
    public void download(String name, HttpServletResponse response) {
        String filePath = null;
        try {
            // 路径规范化：防止 .. 路径穿越攻击
            String normalizedPath = name.replace("/", File.separator).replace("\\", File.separator);
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

            // 根据文件扩展名设置Content-Type
            String extension = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
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
