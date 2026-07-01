package com.yky.blog.api.controller;

import com.yky.blog.admin.service.FileStorageService;
import com.yky.blog.admin.vo.FileUploadVO;
import com.yky.blog.common.annotation.RateLimit;
import com.yky.blog.common.result.Result;
import com.yky.blog.common.satoken.StpUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "前台读者上传")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadApiController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "读者上传图片（评论用，需登录）")
    @RateLimit(name = "reader-upload", window = 60, limit = 10, message = "上传过于频繁，请稍后再试")
    @PostMapping("/image")
    public Result<FileUploadVO> uploadImage(@RequestParam("file") MultipartFile file) {
        StpUserUtil.checkLogin();
        return Result.success(fileStorageService.uploadImage(file, "comment"));
    }
}
