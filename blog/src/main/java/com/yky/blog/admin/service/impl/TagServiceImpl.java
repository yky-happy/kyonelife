package com.yky.blog.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yky.blog.admin.dto.TagSaveDTO;
import com.yky.blog.admin.service.TagService;
import com.yky.blog.admin.vo.TagVO;
import com.yky.blog.common.entity.Tag;
import com.yky.blog.common.exception.BizException;
import com.yky.blog.common.mapper.TagMapper;
import com.yky.blog.common.redis.ArticleCacheEvictor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    private final ArticleCacheEvictor articleCacheEvictor;

    @Override
    public IPage<TagVO> pageTag(int page, int size, String keyword) {
        return baseMapper.selectTagVOPage(new Page<>(page, size), keyword);
    }

    @Override
    public Long saveTag(TagSaveDTO dto) {
        checkNameDuplicate(dto.getName(), null);
        Tag tag = new Tag();
        BeanUtils.copyProperties(dto, tag);
        save(tag);
        return tag.getId();
    }

    @Override
    public void updateTag(Long id, TagSaveDTO dto) {
        checkNameDuplicate(dto.getName(), id);
        Tag tag = new Tag();
        BeanUtils.copyProperties(dto, tag);
        tag.setId(id);
        updateById(tag);
        // 标签名/颜色变更会影响文章卡片上的标签展示，失效文章缓存
        articleCacheEvictor.evictAll();
    }

    @Override
    public void removeTag(Long id) {
        removeById(id);
        articleCacheEvictor.evictAll();
    }

    private void checkNameDuplicate(String name, Long excludeId) {
        boolean exists = lambdaQuery()
                .eq(Tag::getName, name)
                .ne(excludeId != null, Tag::getId, excludeId)
                .exists();
        if (exists) {
            throw new BizException("标签名已存在");
        }
    }
}
