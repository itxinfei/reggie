package com.reggie.module.dining.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.dining.dto.MergeTableDTO;
import com.reggie.module.dining.dto.OpenTableDTO;
import com.reggie.module.dining.dto.SplitBillDTO;
import com.reggie.module.dining.dto.TransferTableDTO;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.vo.TableStatsVO;

import java.util.List;
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

    /**
     * 并台：将多个桌台的订单合并到主桌台
     * <p>流程：校验主桌台和被合并桌台都存在且为占用状态；将各被合并桌台的订单绑定到主桌台；被合并桌台释放。</p>
     *
     * @param dto 并台请求
     */
    void mergeTables(MergeTableDTO dto);

    /**
     * 拆台：将桌台的订单拆分到新桌台
     * <p>流程：校验原桌台为占用且有空余座位；创建新桌台；将部分订单菜品拆分到新桌台订单。</p>
     *
     * @param originalTableId 原桌台 ID
     * @param newTableId      新桌台 ID
     * @param splitOrderIds   需要拆分出去的订单 ID 列表
     */
    void splitTable(Long originalTableId, Long newTableId, List<Long> splitOrderIds);

    /**
     * AA 分账：为指定订单创建拆分子单，支持按份数均分或自定义金额
     * <p>主单状态变更为 SPLIT，各子单独立结算。</p>
     *
     * @param dto 分账请求
     */
    void splitBill(SplitBillDTO dto);
}
