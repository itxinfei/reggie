package com.reggie.module.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.reggie.common.CustomException;
import com.reggie.module.store.mapper.StoreInfoMapper;
import com.reggie.module.store.model.StoreInfo;
import com.reggie.module.store.service.BusinessHoursService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 营业时间与暂停接单服务实现
 *
 * @author reggie
 * @since 2026-09-12
 */
@Slf4j
@Service
public class BusinessHoursServiceImpl implements BusinessHoursService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private StoreInfoMapper storeMapper;

    @Override
    public void checkBusinessHours(Long tenantId) {
        if (tenantId == null) {
            return;
        }

        StoreInfo store = storeMapper.selectById(tenantId);
        if (store == null) {
            return;
        }

        // 1. 暂停接单校验
        if (store.getPauseOrder() != null && store.getPauseOrder() == 1) {
            throw new CustomException("店铺暂停接单，请稍后再试");
        }

        // 2. 营业时间校验（businessHours 为空时降级兼容，不做校验）
        String businessHours = store.getBusinessHours();
        if (StringUtils.isBlank(businessHours)) {
            return;
        }

        // 解析格式：HH:mm-HH:mm（如 "09:00-22:00"）。
        // 配置异常（格式无法解析）时降级跳过校验，不阻断正常下单——
        // 原实现把解析异常重新抛出，导致门店改错一次营业时间格式后，全店所有时段无法下单。
        try {
            String[] parts = businessHours.split("-");
            if (parts.length != 2) {
                log.warn("[营业时间] 格式异常，跳过校验: tenantId={}, businessHours={}", tenantId, businessHours);
                return;
            }

            LocalTime openTime = LocalTime.parse(parts[0].trim(), HH_MM);
            LocalTime closeTime = LocalTime.parse(parts[1].trim(), HH_MM);
            LocalTime now = LocalTime.now();

            boolean inRange;
            // 支持跨午夜营业（如 22:00-06:00）
            if (openTime.isBefore(closeTime)) {
                // 正常区间：09:00-22:00
                inRange = !now.isBefore(openTime) && !now.isAfter(closeTime);
            } else {
                // 跨午夜区间：22:00-06:00 → 当前 >= 22:00 或 <= 06:00
                inRange = !now.isBefore(openTime) || !now.isAfter(closeTime);
            }

            if (!inRange) {
                throw new CustomException("非营业时间，营业时间 " + businessHours);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[营业时间] 解析失败，跳过校验: tenantId={}, businessHours={}", tenantId, businessHours, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void togglePauseOrder(Long tenantId, boolean pause) {
        int pauseVal = pause ? 1 : 0;
        LambdaUpdateWrapper<StoreInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(StoreInfo::getTenantId, tenantId)
               .set(StoreInfo::getPauseOrder, pauseVal);
        storeMapper.update(null, wrapper);
        log.info("[门店管理] 暂停接单状态已更新: tenantId={}, pauseOrder={}", tenantId, pauseVal);
    }
}
