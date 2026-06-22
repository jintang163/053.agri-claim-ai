package com.agri.claim.system.controller;

import com.agri.claim.common.result.R;
import com.agri.claim.system.entity.SysMenu;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    @Operation(summary = "获取菜单树")
    @GetMapping("/tree")
    public R<List<SysMenu>> tree() {
        List<SysMenu> all = buildMockMenus();
        return R.ok(buildTree(all, 0L));
    }

    @Operation(summary = "获取路由菜单")
    @GetMapping("/routers")
    public R<List<SysMenu>> routers(@RequestHeader("X-User-Role") String roleKey) {
        List<SysMenu> all = buildMockMenus();
        return R.ok(buildTree(all, 0L));
    }

    private List<SysMenu> buildTree(List<SysMenu> list, Long parentId) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : list) {
            if (parentId.equals(menu.getParentId())) {
                menu.setChildren(buildTree(list, menu.getId()));
                tree.add(menu);
            }
        }
        return tree;
    }

    private List<SysMenu> buildMockMenus() {
        List<SysMenu> menus = new ArrayList<>();
        long id = 1;

        SysMenu m1 = new SysMenu();
        m1.setId(id++);
        m1.setMenuName("首页");
        m1.setParentId(0L);
        m1.setOrderNum(1);
        m1.setPath("/dashboard");
        m1.setComponent("dashboard/index");
        m1.setMenuType("M");
        m1.setIcon("Odometer");
        menus.add(m1);

        SysMenu m2 = new SysMenu();
        m2.setId(id++);
        m2.setMenuName("影像管理");
        m2.setParentId(0L);
        m2.setOrderNum(2);
        m2.setPath("/image");
        m2.setComponent("Layout");
        m2.setMenuType("M");
        m2.setIcon("Picture");
        menus.add(m2);

        SysMenu m21 = new SysMenu();
        m21.setId(id++);
        m21.setMenuName("影像上传");
        m21.setParentId(m2.getId());
        m21.setOrderNum(1);
        m21.setPath("upload");
        m21.setComponent("image/upload");
        m21.setMenuType("C");
        m21.setPerms("image:upload");
        menus.add(m21);

        SysMenu m22 = new SysMenu();
        m22.setId(id++);
        m22.setMenuName("影像库");
        m22.setParentId(m2.getId());
        m22.setOrderNum(2);
        m22.setPath("list");
        m22.setComponent("image/list");
        m22.setMenuType("C");
        m22.setPerms("image:list");
        menus.add(m22);

        SysMenu m3 = new SysMenu();
        m3.setId(id++);
        m3.setMenuName("定损管理");
        m3.setParentId(0L);
        m3.setOrderNum(3);
        m3.setPath("/assess");
        m3.setComponent("Layout");
        m3.setMenuType("M");
        m3.setIcon("Document");
        menus.add(m3);

        SysMenu m31 = new SysMenu();
        m31.setId(id++);
        m31.setMenuName("定损任务");
        m31.setParentId(m3.getId());
        m31.setOrderNum(1);
        m31.setPath("list");
        m31.setComponent("assess/list");
        m31.setMenuType("C");
        m31.setPerms("assess:list");
        menus.add(m31);

        SysMenu m32 = new SysMenu();
        m32.setId(id++);
        m32.setMenuName("新建定损");
        m32.setParentId(m3.getId());
        m32.setOrderNum(2);
        m32.setPath("create");
        m32.setComponent("assess/create");
        m32.setMenuType("C");
        m32.setPerms("assess:create");
        menus.add(m32);

        SysMenu m4 = new SysMenu();
        m4.setId(id++);
        m4.setMenuName("大屏监控");
        m4.setParentId(0L);
        m4.setOrderNum(4);
        m4.setPath("/monitor");
        m4.setComponent("monitor/index");
        m4.setMenuType("M");
        m4.setIcon("DataBoard");
        menus.add(m4);

        SysMenu m5 = new SysMenu();
        m5.setId(id++);
        m5.setMenuName("系统管理");
        m5.setParentId(0L);
        m5.setOrderNum(5);
        m5.setPath("/system");
        m5.setComponent("Layout");
        m5.setMenuType("M");
        m5.setIcon("Setting");
        menus.add(m5);

        SysMenu m51 = new SysMenu();
        m51.setId(id++);
        m51.setMenuName("用户管理");
        m51.setParentId(m5.getId());
        m51.setOrderNum(1);
        m51.setPath("user");
        m51.setComponent("system/user");
        m51.setMenuType("C");
        m51.setPerms("system:user:list");
        menus.add(m51);

        SysMenu m52 = new SysMenu();
        m52.setId(id++);
        m52.setMenuName("角色管理");
        m52.setParentId(m5.getId());
        m52.setOrderNum(2);
        m52.setPath("role");
        m52.setComponent("system/role");
        m52.setMenuType("C");
        m52.setPerms("system:role:list");
        menus.add(m52);

        SysMenu m53 = new SysMenu();
        m53.setId(id++);
        m53.setMenuName("字典管理");
        m53.setParentId(m5.getId());
        m53.setOrderNum(3);
        m53.setPath("dict");
        m53.setComponent("system/dict");
        m53.setMenuType("C");
        menus.add(m53);

        return menus;
    }
}
