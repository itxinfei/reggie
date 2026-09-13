package com.reggie.module.kds.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 后厨出餐大屏看板：按制作状态分栏，附整体统计。
 *
 * @author reggie
 * @since 2026-09-13
 */
@Data
@Schema(description = "后厨出餐大屏看板")
public class KitchenBoardVO {

    @Schema(description = "待制作列（按接单时间升序，先来先做）")
    private List<KitchenTicketVO> pending = new ArrayList<>();

    @Schema(description = "制作中列")
    private List<KitchenTicketVO> cooking = new ArrayList<>();

    @Schema(description = "待取餐/叫号列")
    private List<KitchenTicketVO> ready = new ArrayList<>();

    @Schema(description = "待制作数量")
    private int pendingCount;

    @Schema(description = "制作中数量")
    private int cookingCount;

    @Schema(description = "待取餐数量")
    private int readyCount;

    @Schema(description = "今日已完成数量")
    private int finishedCount;

    @Schema(description = "加急工单数量（待制作+制作中）")
    private int urgentCount;

    @Schema(description = "平均制作耗时（秒，开始制作→叫号）")
    private long avgCookSeconds;

    @Schema(description = "本次自动拉取新生成的工单数")
    private int pulledCount;
}
