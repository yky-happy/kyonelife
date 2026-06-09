package com.yky.blog.admin.service;

import com.yky.blog.admin.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    FileUploadVO uploadImage(MultipartFile file, String dir);

    FileUploadVO uploadVideo(MultipartFile file, String dir);
}
