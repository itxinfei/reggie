package com.reggie.module.dining.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.dining.mapper.TableAreaMapper;
import com.reggie.module.dining.model.TableArea;
import com.reggie.module.dining.service.TableAreaService;
import org.springframework.stereotype.Service;

/**
 * 桌台区域服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class TableAreaServiceImpl extends ServiceImpl<TableAreaMapper, TableArea> implements TableAreaService {
}
