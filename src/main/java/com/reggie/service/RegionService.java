package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.entity.Region;
import java.util.List;

public interface RegionService extends IService<Region> {

    /**
     * 获取完整的省市区三级树形数据
     * 用于前端级联选择器
     */
    List<Region> getRegionTree();

    /**
     * 根据父级ID查询子级列表
     * @param parentId 父级ID
     * @return 子级地区列表
     */
    List<Region> getChildren(Long parentId);
}
