package com.yky.blog.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.admin.dto.RoleSaveDTO;
import com.yky.blog.common.entity.Role;

import java.util.List;

public interface RoleManageService {

    IPage<Role> pageRole(int page, int size, String keyword);

    List<Role> listAll();

    Long saveRole(RoleSaveDTO dto);

    void updateRole(Long id, RoleSaveDTO dto);

    void removeRole(Long id);

    List<Long> getMenuIds(Long roleId);

    void assignMenus(Long roleId, List<Long> menuIds);
}
