package com.yky.blog.admin.controller;

import com.yky.blog.admin.service.FileStorageService;
import com.yky.blog.admin.vo.FileUploadVO;
import com.yky.blog.common.annotation.OperationLogRecord;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "后台文件上传")
@RestController
@RequestMapping("/admin/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传图片")
    @OperationLogRecord(module = "文件上传", operation = "上传图片")
    @PostMapping("/upload")
    public Result<FileUploadVO> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "dir", defaultValue = "article") String dir) {
        return Result.success(fileStorageService.uploadImage(file, dir));
    }

    @Operation(summary = "上传视频")
    @OperationLogRecord(module = "文件上传", operation = "上传视频")
    @PostMapping("/upload/video")
    public Result<FileUploadVO> uploadVideo(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "dir", defaultValue = "video") String dir) {
        return Result.success(fileStorageService.uploadVideo(file, dir));
    }
}
