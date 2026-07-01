package com.yky.blog.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.admin.vo.AdminUserVO;

public interface UserAdminService {

    IPage<AdminUserVO> page(int page, int size, String keyword, Integer status);

    /** 封禁/解封：status 1正常 0封禁 */
    void updateStatus(Long id, Integer status);

    /** 删除读者（连带其评论） */
    void delete(Long id);
}
