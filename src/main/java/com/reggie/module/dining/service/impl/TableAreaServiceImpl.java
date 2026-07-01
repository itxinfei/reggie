package com.reggie.module.dining.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.dining.mapper.TableAreaMapper;
import com.reggie.module.dining.model.TableArea;
import com.reggie.module.dining.service.TableAreaService;
import org.springframework.stereotype.Service;

@Service
public class TableAreaServiceImpl extends ServiceImpl<TableAreaMapper, TableArea> implements TableAreaService {
}
