package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.inventory.mapper.MaterialCategoryMapper;
import com.reggie.module.inventory.model.MaterialCategory;
import com.reggie.module.inventory.service.MaterialCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MaterialCategoryServiceImpl extends ServiceImpl<MaterialCategoryMapper, MaterialCategory> implements MaterialCategoryService {
}
