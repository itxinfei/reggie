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

    @Value("${reggie.path}")
    private String basePath;

    /**
     * 文件上传
     *
     * @param file
     * @return 文件上传的目录改为项目运行的根目录
     */
    @PostMapping("/upload")
    @Operation(summary = "文件上传", description = "上传图片文件")
    @Parameter(name = "file", description = "上传的文件", required = true)
    public R<String> upload(MultipartFile file) {
        // 1. 校验文件是否为空
        if (file.isEmpty()) {
            return R.error("上传文件不能为空");
        }

        // 2. 校验文件类型（仅允许图片格式）
        String originalFilename = file.getOriginalFilename();
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
        //String originalFilename = file.getOriginalFilename();//abc.jpg
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        //使用UUID重新生成文件名，防止文件名称重复造成文件覆盖
        String fileName = UUID.randomUUID().toString() + suffix;//dfsdfdfd.jpg

        // 保存到 images/dishes/ 子目录，与数据库中的路径格式一致
        String subDir = "images/dishes/";
        File dir = new File(basePath + subDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            file.transferTo(new File(basePath + subDir + fileName));
        } catch (IOException e) {
            log.error("文件上传失败", e);
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
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(basePath + name));

            //输出流，通过输出流将文件写回浏览器
            ServletOutputStream outputStream = response.getOutputStream();

            response.setContentType("image/jpeg");

            int len = 0;
            byte[] bytes = new byte[BUFFER_SIZE];
            while ((len = fileInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, len);
                outputStream.flush();
            }

            //关闭资源
            outputStream.close();
            fileInputStream.close();
        } catch (Exception e) {
            log.error("文件下载失败", e);
        }

    }
}
