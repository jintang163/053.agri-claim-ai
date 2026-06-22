package com.agri.claim.ai.controller;

import com.agri.claim.ai.dto.DroneFlightTemplateDTO;
import com.agri.claim.ai.entity.DroneFlightTemplate;
import com.agri.claim.ai.service.DroneFlightTemplateService;
import com.agri.claim.common.core.page.PageResult;
import com.agri.claim.common.result.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "无人机飞行模板")
@RestController
@RequestMapping("/ai/drone/template")
@RequiredArgsConstructor
public class DroneFlightTemplateController {

    private final DroneFlightTemplateService templateService;

    @Operation(summary = "创建飞行模板")
    @PostMapping
    public R<DroneFlightTemplate> save(@Valid @RequestBody DroneFlightTemplateDTO dto) {
        DroneFlightTemplate template = templateService.saveTemplate(dto);
        return R.ok("模板创建成功", template);
    }

    @Operation(summary = "更新飞行模板")
    @PutMapping
    public R<Void> update(@Valid @RequestBody DroneFlightTemplateDTO dto) {
        boolean result = templateService.updateTemplate(dto);
        return result ? R.ok("模板更新成功") : R.fail("模板更新失败");
    }

    @Operation(summary = "删除飞行模板")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        boolean result = templateService.deleteTemplate(id);
        return result ? R.ok("模板删除成功") : R.fail("模板删除失败");
    }

    @Operation(summary = "获取飞行模板详情")
    @GetMapping("/{id}")
    public R<DroneFlightTemplate> getById(@PathVariable Long id) {
        DroneFlightTemplate template = templateService.getTemplateById(id);
        return R.ok(template);
    }

    @Operation(summary = "获取当前用户模板列表")
    @GetMapping("/list")
    public R<List<DroneFlightTemplate>> list() {
        List<DroneFlightTemplate> list = templateService.listByUser();
        return R.ok(list);
    }

    @Operation(summary = "分页查询飞行模板")
    @GetMapping("/page")
    public R<PageResult<DroneFlightTemplate>> page(
            @RequestParam(required = false) String keyword) {
        IPage<DroneFlightTemplate> page = templateService.pageList(keyword);
        return R.ok(PageResult.of(page));
    }
}
