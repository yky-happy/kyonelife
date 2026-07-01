package com.yky.blog.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.admin.vo.AdminCommentVO;

public interface CommentAdminService {

    IPage<AdminCommentVO> page(int page, int size, Long articleId, String keyword);

    /** 删除评论（连带其回复） */
    void delete(Long id);

    /** 隐藏/显示评论：status 1正常 0隐藏 */
    void updateStatus(Long id, Integer status);
}
