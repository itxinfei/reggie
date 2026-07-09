package com.reggie.module.export.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.entity.*;
import com.reggie.module.export.util.ExportUtil;
import com.reggie.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 数据导出Controller
 * 统一提供各业务模块的Excel和PDF导出功能
 *
 * @author Reggie Team
 */
@Slf4j
@RestController
@RequestMapping("/export")
public class ExportController {

    @Resource
    private OrderService orderService;

    @Resource
    private OrderDetailService orderDetailService;

    @Resource
    private DishService dishService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private EmployeeService employeeService;

    // ==================== 订单导出 ====================

    /**
     * 导出订单数据 - Excel
     */
    @GetMapping("/orders/excel")
    public void exportOrdersExcel(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) {

        List<Orders> orders = queryOrders(startDate, endDate, status);
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("number", "订单号");
        columns.put("userName", "用户名");
        columns.put("phone", "手机号");
        columns.put("address", "地址");
        columns.put("amount", "实收金额");
        columns.put("status", "订单状态");
        columns.put("payMethod", "支付方式");
        columns.put("orderTime", "下单时间");

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Orders order : orders) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("number", order.getNumber());
            row.put("userName", order.getUserName());
            row.put("phone", order.getPhone());
            row.put("address", order.getAddress());
            row.put("amount", (order.getAmount() != null ? order.getAmount().doubleValue() / 100 : 0) + "元");
            row.put("status", getOrderStatusName(order.getStatus()));
            row.put("payMethod", order.getPayMethod() != null ? (order.getPayMethod() == 1 ? "微信支付" : "支付宝") : "");
            row.put("orderTime", order.getOrderTime());
            dataList.add(row);
        }

        ExportUtil.exportExcel(response, "订单数据", columns, dataList);
    }

    /**
     * 导出订单数据 - PDF
     */
    @GetMapping("/orders/pdf")
    public void exportOrdersPdf(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) {

        List<Orders> orders = queryOrders(startDate, endDate, status);
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("number", "订单号");
        columns.put("userName", "用户名");
        columns.put("phone", "手机号");
        columns.put("amount", "实收金额");
        columns.put("status", "订单状态");
        columns.put("orderTime", "下单时间");

        List<Map<String, Object>> dataList = new ArrayList<>();
        double totalAmount = 0;
        for (Orders order : orders) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("number", order.getNumber());
            row.put("userName", order.getUserName());
            row.put("phone", order.getPhone());
            row.put("amount", (order.getAmount() != null ? order.getAmount().doubleValue() / 100 : 0) + "元");
            row.put("status", getOrderStatusName(order.getStatus()));
            row.put("orderTime", order.getOrderTime());
            dataList.add(row);
            if (order.getAmount() != null) {
                totalAmount += order.getAmount().doubleValue() / 100;
            }
        }

        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("订单总数", String.valueOf(orders.size()));
        summary.put("总金额", "¥" + String.format("%.2f", totalAmount));
        summary.put("日期范围", (startDate != null ? startDate.toString() : "不限") +
                " ~ " + (endDate != null ? endDate.toString() : "不限"));

        ExportUtil.exportPdf(response, "订单报表", "瑞吉外卖 - 订单数据报表",
                columns, dataList, summary);
    }

    // ==================== 菜品导出 ====================

    /**
     * 导出菜品数据 - Excel
     */
    @GetMapping("/dishes/excel")
    public void exportDishesExcel(
            @RequestParam(required = false) Long categoryId,
            HttpServletResponse response) {

        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("name", "菜品名称");
        columns.put("categoryName", "分类");
        columns.put("price", "价格");
        columns.put("status", "状态");
        columns.put("description", "描述");
        columns.put("createTime", "创建时间");

        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getIsDeleted, 0);
        if (categoryId != null) {
            wrapper.eq(Dish::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Dish::getCreateTime);
        List<Dish> dishes = dishService.list(wrapper);

        // 获取分类名称映射
        Map<Long, String> categoryMap = new HashMap<>();
        List<Category> categories = categoryService.list();
        for (Category c : categories) {
            categoryMap.put(c.getId(), c.getName());
        }

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Dish dish : dishes) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", dish.getName());
            row.put("categoryName", categoryMap.getOrDefault(dish.getCategoryId(), "未知"));
            row.put("price", (dish.getPrice() != null ? dish.getPrice().doubleValue() / 100 : 0) + "元");
            row.put("status", dish.getStatus() == 1 ? "在售" : "停售");
            row.put("description", dish.getDescription() != null ? dish.getDescription() : "");
            row.put("createTime", dish.getCreateTime());
            dataList.add(row);
        }

        ExportUtil.exportExcel(response, "菜品数据", columns, dataList);
    }

    /**
     * 导出菜品数据 - PDF
     */
    @GetMapping("/dishes/pdf")
    public void exportDishesPdf(
            @RequestParam(required = false) Long categoryId,
            HttpServletResponse response) {

        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("name", "菜品名称");
        columns.put("categoryName", "分类");
        columns.put("price", "价格");
        columns.put("status", "状态");
        columns.put("createTime", "创建时间");

        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getIsDeleted, 0);
        if (categoryId != null) {
            wrapper.eq(Dish::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Dish::getCreateTime);
        List<Dish> dishes = dishService.list(wrapper);

        Map<Long, String> categoryMap = new HashMap<>();
        for (Category c : categoryService.list()) {
            categoryMap.put(c.getId(), c.getName());
        }

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Dish dish : dishes) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", dish.getName());
            row.put("categoryName", categoryMap.getOrDefault(dish.getCategoryId(), "未知"));
            row.put("price", (dish.getPrice() != null ? dish.getPrice().doubleValue() / 100 : 0) + "元");
            row.put("status", dish.getStatus() == 1 ? "在售" : "停售");
            row.put("createTime", dish.getCreateTime());
            dataList.add(row);
        }

        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("菜品总数", String.valueOf(dishes.size()));

        ExportUtil.exportPdf(response, "菜品报表", "瑞吉外卖 - 菜品数据报表",
                columns, dataList, summary);
    }

    // ==================== 员工导出 ====================

    /**
     * 导出员工数据 - Excel
     */
    @GetMapping("/employees/excel")
    public void exportEmployeesExcel(HttpServletResponse response) {
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("name", "姓名");
        columns.put("username", "账号");
        columns.put("phone", "手机号");
        columns.put("sex", "性别");
        columns.put("status", "状态");
        columns.put("createTime", "入职时间");

        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Employee::getCreateTime);
        List<Employee> employees = employeeService.list(wrapper);

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Employee emp : employees) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", emp.getName());
            row.put("username", emp.getUsername());
            row.put("phone", emp.getPhone() != null ? emp.getPhone() : "");
            row.put("sex", "1".equals(emp.getSex()) ? "男" : ("2".equals(emp.getSex()) ? "女" : ""));
            row.put("status", emp.getStatus() == 1 ? "正常" : "禁用");
            row.put("createTime", emp.getCreateTime());
            dataList.add(row);
        }

        ExportUtil.exportExcel(response, "员工数据", columns, dataList);
    }

    // ==================== 私有方法 ====================

    /**
     * 查询订单列表
     */
    private List<Orders> queryOrders(LocalDate startDate, LocalDate endDate, Integer status) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null) {
            wrapper.ge(Orders::getOrderTime, LocalDateTime.of(startDate, LocalTime.MIN));
        }
        if (endDate != null) {
            wrapper.le(Orders::getOrderTime, LocalDateTime.of(endDate, LocalTime.MAX));
        }
        if (status != null) {
            wrapper.eq(Orders::getStatus, status);
        }
        wrapper.orderByDesc(Orders::getOrderTime);
        return orderService.list(wrapper);
    }

    /**
     * 订单状态中文名
     */
    private String getOrderStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "待付款";
            case 2: return "待接单";
            case 3: return "已接单";
            case 4: return "派送中";
            case 5: return "已完成";
            case 6: return "已取消";
            default: return "其他";
        }
    }
}
