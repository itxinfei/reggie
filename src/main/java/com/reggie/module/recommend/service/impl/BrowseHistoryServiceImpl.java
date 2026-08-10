package com.reggie.module.recommend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.recommend.mapper.BrowseHistoryMapper;
import com.reggie.module.recommend.model.BrowseHistory;
import com.reggie.module.recommend.service.BrowseHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 浏览历史服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
/**
 * BrowseHistory service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BrowseHistoryServiceImpl extends ServiceImpl<BrowseHistoryMapper, BrowseHistory>
        implements BrowseHistoryService {

    /** 浏览历史Mapper */
    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;

    @Override
    public void recordBrowse(Long userId, Integer targetType, Long targetId,
                              String targetName, Integer duration, Integer actionType) {
        if (userId == null || targetId == null) return;

        BrowseHistory history = new BrowseHistory();
        history.setUserId(userId);
        history.setTargetType(targetType);
        history.setTargetId(targetId);
        history.setTargetName(targetName);
        history.setDurationSeconds(duration != null ? duration : 0);
        history.setActionType(actionType != null ? actionType : BrowseHistory.ACTION_VIEW);

        save(history);
        log.debug("[浏览记录] 用户{} {}了{}", userId,
                getActionName(actionType), targetName);
    }

    @Override
    public List<BrowseHistory> getRecentHistory(Long userId, int limit) {
        return browseHistoryMapper.findRecentByUserId(userId, limit);
    }

    @Override
    public List<Map<String, Object>> getTopCategories(Long userId, int limit) {
        return browseHistoryMapper.findTopViewedDishes(userId, limit);
    }

    private String getActionName(Integer actionType) {
        if (actionType == null) return "浏览";
        switch (actionType) {
            case BrowseHistory.ACTION_FAVORITE: return "收藏";
            case BrowseHistory.ACTION_ADD_CART: return "加购";
            case BrowseHistory.ACTION_SHARE: return "分享";
            default: return "浏览";
        }
    }
}



