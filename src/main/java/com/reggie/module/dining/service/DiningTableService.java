package com.reggie.module.dining.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.dining.dto.OpenTableDTO;
import com.reggie.module.dining.dto.TransferTableDTO;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.vo.TableStatsVO;

import java.util.Map;

/**
 * <p>
 * 堂食桌台服务接口
 * </p>
 * <p>提供桌台状态管理、分页查询等功能</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface DiningTableService extends IService<DiningTable> {

    /**
     * 更改桌台状态
     *
     * @param tableId 桌台ID
     * @param status  目标状态
     */
    void changeStatus(Long tableId, String status);

    /**
     * 开台：绑定订单到桌台，桌台状态改为占用
     *
     * @param dto 开台请求
     */
    void openTable(OpenTableDTO dto);

    /**
     * 转台：订单从原桌台迁移到新桌台
     *
     * @param dto 转台请求
     */
    void transferTable(TransferTableDTO dto);

    /**
     * 分页查询桌台信息（关联区域名称）
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页桌台列表
     */
    Page<DiningTable> pageWithArea(int page, int pageSize);

    /**
     * 分页查询桌台信息（关联区域名称，支持筛选）
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param name     桌台名称（可选，模糊匹配）
     * @param areaId   区域ID（可选）
     * @param status   桌台状态（可选）
     * @return 分页桌台列表
     */
    Page<DiningTable> pageWithArea(int page, int pageSize, String name, Long areaId, String status);

    /**
     * 桌台统计（按状态分类计数）
     *
     * @return 桌台统计
     */
    TableStatsVO tableStats();

    /**
     * 桌台区域统计（总数 + 最大容量区域）
     * <p>域4 改造：从 DiningTableController 下沉，Controller 不再直接操作 Mapper</p>
     *
     * @return 统计结果，包含 totalTables/maxTablesArea
     */
    Map<String, Object> areaStats();
}
