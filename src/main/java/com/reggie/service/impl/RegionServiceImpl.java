package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.entity.Region;
import com.reggie.mapper.RegionMapper;
import com.reggie.service.RegionService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region> implements RegionService {

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
