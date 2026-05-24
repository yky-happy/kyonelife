package com.yky.blog.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yky.blog.admin.dto.TagSaveDTO;
import com.yky.blog.admin.vo.TagVO;
import com.yky.blog.common.entity.Tag;

public interface TagService extends IService<Tag> {

    IPage<TagVO> pageTag(int page, int size, String keyword);

    Long saveTag(TagSaveDTO dto);

    void updateTag(Long id, TagSaveDTO dto);

    void removeTag(Long id);
}
