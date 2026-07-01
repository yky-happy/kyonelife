package com.yky.blog.admin.service;

import com.yky.blog.admin.dto.MenuSaveDTO;
import com.yky.blog.admin.vo.MenuTreeVO;

import java.util.List;

public interface MenuManageService {

    List<MenuTreeVO> tree();

    Long saveMenu(MenuSaveDTO dto);

    void updateMenu(Long id, MenuSaveDTO dto);

    void removeMenu(Long id);
}
