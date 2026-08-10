package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.entity.Region;
import com.reggie.mapper.RegionMapper;
import com.reggie.service.RegionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;

/**
 * 地区服务实现类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region> implements RegionService {

    /**
     * 获取地区树形结构
     *
     * @return 地区树形列表
     */
    @Override
    public List<Region> getRegionTree() {
        // 查询所有未删除的地区，按sort排序
        LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Region::getSort);
        List<Region> allRegions = this.list(wrapper);

        // 构建树形结构
        List<Region> rootList = new ArrayList<>();
        for (Region region : allRegions) {
            if (region.getParentId() == null || region.getParentId() == 0L) {
                rootList.add(region);
            }
        }
        for (Region root : rootList) {
            buildChildren(root, allRegions);
        }
        return rootList;
    }

    /**
     * 获取子地区列表
     *
     * @param parentId 父级ID
     * @return 子地区列表
     */
    @Override
    public List<Region> getChildren(Long parentId) {
        LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Region::getParentId, parentId)
               .orderByAsc(Region::getSort);
        return this.list(wrapper);
    }

    /**
     * 递归构建子节点
     */
    private void buildChildren(Region parent, List<Region> allRegions) {
        List<Region> children = new ArrayList<>();
        for (Region region : allRegions) {
            if (region.getParentId() != null && region.getParentId().equals(parent.getId())) {
                children.add(region);
                buildChildren(region, allRegions);
            }
        }
        parent.setChildren(children.isEmpty() ? null : children);
    }
}

