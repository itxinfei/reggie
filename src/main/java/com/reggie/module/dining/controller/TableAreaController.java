package com.reggie.module.dining.controller;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.dining.model.TableArea;
import com.reggie.module.dining.service.TableAreaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.reggie.common.RateLimit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 堂食区域管理控制器
 * 提供桌台区域的增删改查接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/api/dining/area")
@Tag(name = "堂食区域管理")
@RequireEmployee
public class TableAreaController {

    @Autowired
    private TableAreaService tableAreaService;

    /**
     * 分页查询桌台区域列表
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询桌台区域列表")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    public R<Page<TableArea>> page(@RequestParam(defaultValue = "1") @Min(1) int page, @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        Page<TableArea> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<TableArea> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(TableArea::getSort);
        tableAreaService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    /**
     * 新增桌台区域
     * @param area 区域信息
     * @return 新增区域信息
     */
    @PostMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增区域", description = "创建新的桌台区域")
    public R<TableArea> save(@RequestBody TableArea area) {
        log.info("新增区域: {}", area.getName());
        area.setTenantId(BaseContext.getCurrentTenantId());
        tableAreaService.save(area);
        return R.success(area);
    }

    /**
     * 修改桌台区域
     * <p>租户安全：接收实体后剥离 tenantId，再校验归属当前租户后更新业务字段。</p>
     *
     * @param area 区域信息（tenantId 字段被服务端覆盖）
     * @return 操作结果
     */
    @PutMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改区域", description = "更新桌台区域信息")
    public R<String> update(@RequestBody TableArea area) {
        log.info("修改区域: {}", area.getId());
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        // 先查询现有记录并校验归属
        TableArea exist = tableAreaService.getById(area.getId());
        if (exist == null) {
            throw new CustomException("桌台区域不存在");
        }
        if (!tenantId.equals(exist.getTenantId())) {
            throw new CustomException("桌台区域不属于当前租户");
        }
        // 仅更新业务字段，强制覆盖 tenantId 为当前租户
        exist.setName(area.getName());
        exist.setSort(area.getSort());
        tableAreaService.updateById(exist);
        return R.success("修改区域成功");
    }

    @DeleteMapping("/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除区域", description = "根据ID删除桌台区域（先校验租户归属）")
    @Parameter(name = "id", description = "区域ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        log.info("删除区域: {}", id);
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        TableArea exist = tableAreaService.getById(id);
        if (exist == null) {
            throw new CustomException("桌台区域不存在");
        }
        if (!tenantId.equals(exist.getTenantId())) {
            throw new CustomException("桌台区域不属于当前租户");
        }
        tableAreaService.removeById(id);
        return R.success("删除区域成功");
    }

    /**
     * 查询所有桌台区域
     * @return 区域列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询所有区域", description = "查询所有桌台区域列表")
    public R<List<TableArea>> list() {
        LambdaQueryWrapper<TableArea> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(TableArea::getSort);
        List<TableArea> list = tableAreaService.list(qw);
        return R.success(list);
    }

    /**
     * 根据ID查询桌台区域
     * @param id 区域ID
     * @return 区域详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询区域", description = "根据ID查询桌台区域详情")
    @Parameter(name = "id", description = "区域ID", required = true)
    public R<TableArea> getById(@PathVariable Long id) {
        TableArea area = tableAreaService.getById(id);
        if (area != null) {
            return R.success(area);
        }
        return R.error("没有查询到对应区域");
    }

    /**
     * 获取筛选下拉选项（区域名称列表）
     * <p>从数据库动态查询所有区域名称，供前端下拉框使用</p>
     *
     * @return 包含 names 列表的 Map
     */
    @GetMapping("/options")
    @Operation(summary = "筛选选项", description = "获取所有区域名称，供搜索条件下拉框使用")
    public R<Map<String, List<String>>> options() {
        LambdaQueryWrapper<TableArea> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(TableArea::getSort);
        List<TableArea> list = tableAreaService.list(qw);

        Set<String> nameSet = new HashSet<>();
        for (TableArea area : list) {
            if (area.getName() != null && !area.getName().isEmpty()) {
                nameSet.add(area.getName());
            }
        }

        Map<String, List<String>> result = new HashMap<>();
        result.put("names", new ArrayList<>(nameSet));
        return R.success(result);
    }
}

