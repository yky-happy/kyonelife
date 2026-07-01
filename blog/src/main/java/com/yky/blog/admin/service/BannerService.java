package com.yky.blog.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.admin.dto.BannerSaveDTO;
import com.yky.blog.common.entity.Banner;

import java.util.List;

public interface BannerService {

    IPage<Banner> pageBanner(int page, int size);

    /** 前台：启用的轮播，按 sort 倒序。 */
    List<Banner> listEnabled();

    Long saveBanner(BannerSaveDTO dto);

    void updateBanner(Long id, BannerSaveDTO dto);

    void updateStatus(Long id, Integer status);

    void removeBanner(Long id);
}
