package com.yky.blog.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yky.blog.admin.dto.CollectionSaveDTO;
import com.yky.blog.admin.service.CollectionService;
import com.yky.blog.admin.vo.CollectionVO;
import com.yky.blog.common.entity.Collection;
import com.yky.blog.common.exception.BizException;
import com.yky.blog.common.mapper.CollectionMapper;
import com.yky.blog.common.redis.ArticleCacheEvictor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollectionServiceImpl extends ServiceImpl<CollectionMapper, Collection> implements CollectionService {

    private final ArticleCacheEvictor articleCacheEvictor;

    @Override
    public IPage<CollectionVO> pageCollection(int page, int size, String keyword) {
        return baseMapper.selectCollectionVOPage(new Page<>(page, size), keyword);
    }

    @Override
    public Long saveCollection(CollectionSaveDTO dto) {
        checkNameDuplicate(dto.getName(), null);
        Collection collection = new Collection();
        BeanUtils.copyProperties(dto, collection);
        save(collection);
        return collection.getId();
    }

    @Override
    public void updateCollection(Long id, CollectionSaveDTO dto) {
        checkNameDuplicate(dto.getName(), id);
        Collection collection = new Collection();
        BeanUtils.copyProperties(dto, collection);
        collection.setId(id);
        updateById(collection);
        // 合集名称变更会影响文章卡片上的合集展示，失效文章缓存
        articleCacheEvictor.evictAll();
    }

    @Override
    public void removeCollection(Long id) {
        removeById(id);
        articleCacheEvictor.evictAll();
    }

    private void checkNameDuplicate(String name, Long excludeId) {
        boolean exists = lambdaQuery()
                .eq(Collection::getName, name)
                .ne(excludeId != null, Collection::getId, excludeId)
                .exists();
        if (exists) {
            throw new BizException("合集名称已存在");
        }
    }
}
