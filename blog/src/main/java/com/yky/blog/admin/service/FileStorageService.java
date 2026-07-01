package com.yky.blog.admin.service;

import com.yky.blog.admin.vo.FileItemVO;
import com.yky.blog.admin.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {

    FileUploadVO uploadImage(MultipartFile file, String dir);

    FileUploadVO uploadVideo(MultipartFile file, String dir);

    /** 列出某目录下的文件（按目录前缀）。 */
    List<FileItemVO> listFiles(String dir, int limit);

    /** 删除指定对象。 */
    void deleteFile(String objectName);
}
