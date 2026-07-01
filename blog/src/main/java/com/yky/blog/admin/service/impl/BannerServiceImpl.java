package com.yky.blog.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yky.blog.admin.dto.BannerSaveDTO;
import com.yky.blog.admin.service.BannerService;
import com.yky.blog.common.entity.Banner;
import com.yky.blog.common.exception.BizException;
import com.yky.blog.common.mapper.BannerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

    @Override
    public IPage<Banner> pageBanner(int page, int size) {
        return page(new Page<>(page, size),
                new LambdaQueryWrapper<Banner>().orderByDesc(Banner::getSort).orderByDesc(Banner::getId));
    }

    @Override
    public List<Banner> listEnabled() {
        return list(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, 1)
                .orderByDesc(Banner::getSort)
                .orderByDesc(Banner::getId));
    }

    @Override
    public Long saveBanner(BannerSaveDTO dto) {
        Banner banner = new Banner();
        BeanUtils.copyProperties(dto, banner);
        if (banner.getSort() == null) banner.setSort(0);
        if (banner.getStatus() == null) banner.setStatus(1);
        save(banner);
        return banner.getId();
    }

    @Override
    public void updateBanner(Long id, BannerSaveDTO dto) {
        if (getById(id) == null) {
            throw new BizException("轮播不存在");
        }
        Banner banner = new Banner();
        BeanUtils.copyProperties(dto, banner);
        banner.setId(id);
        updateById(banner);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态值不正确");
        }
        if (getById(id) == null) {
            throw new BizException("轮播不存在");
        }
        Banner banner = new Banner();
        banner.setId(id);
        banner.setStatus(status);
        updateById(banner);
    }

    @Override
    public void removeBanner(Long id) {
        removeById(id);
    }
}
