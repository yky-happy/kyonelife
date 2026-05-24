package com.yky.blog.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yky.blog.admin.dto.CollectionSaveDTO;
import com.yky.blog.admin.vo.CollectionVO;
import com.yky.blog.common.entity.Collection;

public interface CollectionService extends IService<Collection> {

    IPage<CollectionVO> pageCollection(int page, int size, String keyword);

    Long saveCollection(CollectionSaveDTO dto);

    void updateCollection(Long id, CollectionSaveDTO dto);

    void removeCollection(Long id);
}
