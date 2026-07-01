package com.yky.blog.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yky.blog.admin.dto.MenuSaveDTO;
import com.yky.blog.admin.service.MenuManageService;
import com.yky.blog.admin.vo.MenuTreeVO;
import com.yky.blog.auth.service.PermissionService;
import com.yky.blog.common.entity.Menu;
import com.yky.blog.common.entity.RoleMenu;
import com.yky.blog.common.exception.BizException;
import com.yky.blog.common.mapper.MenuMapper;
import com.yky.blog.common.mapper.RoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MenuManageServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuManageService {

    private final RoleMenuMapper roleMenuMapper;
    private final PermissionService permissionService;

    @Override
    public List<MenuTreeVO> tree() {
        List<Menu> all = list(new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSort));
        Map<Long, MenuTreeVO> idToNode = new LinkedHashMap<>();
        for (Menu menu : all) {
            idToNode.put(menu.getId(), toVO(menu));
        }
        List<MenuTreeVO> roots = new ArrayList<>();
        for (MenuTreeVO node : idToNode.values()) {
            MenuTreeVO parent = (node.getParentId() == null || node.getParentId() == 0)
                    ? null : idToNode.get(node.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    @Override
    public Long saveMenu(MenuSaveDTO dto) {
        Menu menu = new Menu();
        BeanUtils.copyProperties(dto, menu);
        fillDefaults(menu);
        save(menu);
        permissionService.clearAllCache();
        return menu.getId();
    }

    @Override
    public void updateMenu(Long id, MenuSaveDTO dto) {
        if (getById(id) == null) {
            throw new BizException("菜单不存在");
        }
        Menu menu = new Menu();
        BeanUtils.copyProperties(dto, menu);
        menu.setId(id);
        fillDefaults(menu);
        updateById(menu);
        permissionService.clearAllCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMenu(Long id) {
        if (count(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id)) > 0) {
            throw new BizException("存在子菜单，请先删除子菜单");
        }
        removeById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getMenuId, id));
        permissionService.clearAllCache();
    }

    private void fillDefaults(Menu menu) {
        if (menu.getParentId() == null) menu.setParentId(0L);
        if (menu.getSort() == null) menu.setSort(0);
        if (menu.getHidden() == null) menu.setHidden(0);
    }

    private MenuTreeVO toVO(Menu menu) {
        MenuTreeVO vo = new MenuTreeVO();
        BeanUtils.copyProperties(menu, vo);
        return vo;
    }
}
