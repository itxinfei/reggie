package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.inventory.mapper.PurchaseOrderDetailMapper;
import com.reggie.module.inventory.model.PurchaseOrderDetail;
import com.reggie.module.inventory.service.PurchaseOrderDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PurchaseOrderDetailServiceImpl extends ServiceImpl<PurchaseOrderDetailMapper, PurchaseOrderDetail> implements PurchaseOrderDetailService {
}
