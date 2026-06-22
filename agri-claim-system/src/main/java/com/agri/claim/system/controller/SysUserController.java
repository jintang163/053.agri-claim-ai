package com.agri.claim.system.controller;

import com.agri.claim.common.core.page.PageQuery;
import com.agri.claim.common.core.page.PageResult;
import com.agri.claim.common.result.R;
import com.agri.claim.system.entity.SysUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.agri.claim.system.mapper.SysUserMapper;

import java.util.List;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @Operation(summary = "分页查询用户列表")
    @GetMapping("/list")
    public R<PageResult<SysUser>> list(SysUser query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> page = sysUserService.page(PageQuery.build().toPage(), wrapper);
        return R.ok(PageResult.of(page));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public R<SysUser> getById(@PathVariable Long id) {
        return R.ok(sysUserService.getById(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public R<Void> add(@RequestBody SysUser user) {
        sysUserService.save(user);
        return R.ok("新增成功");
    }

    @Operation(summary = "修改用户")
    @PutMapping
    public R<Void> edit(@RequestBody SysUser user) {
        sysUserService.updateById(user);
        return R.ok("修改成功");
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        sysUserService.removeByIds(ids);
        return R.ok("删除成功");
    }

    public interface SysUserService extends IService<SysUser> {}

    @org.springframework.stereotype.Service
    public static class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {}
}
