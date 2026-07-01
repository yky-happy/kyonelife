package com.yky.blog.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.admin.dto.AdminSaveDTO;
import com.yky.blog.admin.dto.AdminUpdateDTO;
import com.yky.blog.admin.vo.AdminVO;

public interface AdminManageService {

    IPage<AdminVO> pageAdmin(int page, int size, String keyword);

    Long saveAdmin(AdminSaveDTO dto);

    void updateAdmin(Long id, AdminUpdateDTO dto);

    void updateStatus(Long id, Integer status);

    void removeAdmin(Long id);
}
