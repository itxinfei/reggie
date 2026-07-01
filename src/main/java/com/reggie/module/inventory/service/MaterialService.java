package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.inventory.model.Material;
import java.util.List;

public interface MaterialService extends IService<Material> {
    Page<Material> pageWithCategory(int page, int pageSize);
    List<Material> checkWarning();
}
