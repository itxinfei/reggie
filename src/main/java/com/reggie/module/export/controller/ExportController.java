package com.reggie.module.export.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.auth.service.EmployeeService;
import com.reggie.module.auth.model.Employee;
import com.reggie.module.order.model.Orders;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.category.model.Category;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.category.service.CategoryService;
import com.reggie.enums.OrderStatus;
import com.reggie.module.export.util.ExportUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据导出Controller
 * 统一提供各业务模块的Excel和PDF导出功能
 * 修改点：全部导出方法添加try-catch，数据构建逻辑抽取为公共方法消除重复代码
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/export")
@Tag(name = "数据导出", description = "订单、菜品、员工等业务数据的Excel/PDF导出功能")
@RequireEmployee
public class ExportController {

    /** 订单服务 */
    @Resource
    private OrderService orderService;

    /** 订单明细服务 */
    @Resource
    private OrderDetailService orderDetailService;

    /** 菜品服务 */
    @Resource
    private DishService dishService;

    /** 分类服务 */
    @Resource
    private CategoryService categoryService;

    /** 员工服务 */
    @Resource
    private EmployeeService employeeService;

    /** 文件名日期格式 */
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ==================== 订单导出 ====================

    /**
     * 导出订单数据 - Excel
     *
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @param status    订单状态（可选）
     * @return Excel文件流
     */
    @GetMapping("/orders/excel")
    @Operation(summary = "导出订单Excel", description = "导出订单数据为Excel文件，支持按日期范围和订单状态筛选")
    public ResponseEntity<?> exportOrdersExcel(
                        @Parameter(description = "开始日期（可选）") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期（可选）") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "订单状态（可选）") @RequestParam(required = false) Integer status) {

        try {
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

            List<Map<String, Object>> dataList = buildOrderDataList(orders, true);
            byte[] bytes = ExportUtil.generateExcelBytes(columns, dataList);
            return buildFileResponse(bytes, "订单数据", "xlsx");
        } catch (Exception e) {
            log.error("导出订单Excel失败: startDate={}, endDate={}", startDate, endDate, e);
            return buildErrorResponse("订单Excel导出失败，请稍后重试");
        }
    }

    /**
     * 导出订单数据 - PDF
     *
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @param status    订单状态（可选）
     * @return PDF文件流
     */
    @GetMapping("/orders/pdf")
    @Operation(summary = "导出订单PDF", description = "导出订单数据为PDF报表，支持按日期范围和订单状态筛选")
    public ResponseEntity<?> exportOrdersPdf(
                        @Parameter(description = "开始日期（可选）") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期（可选）") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "订单状态（可选）") @RequestParam(required = false) Integer status) {

        try {
            List<Orders> orders = queryOrders(startDate, endDate, status);

            LinkedHashMap<String, String> columns = new LinkedHashMap<>();
            columns.put("number", "订单号");
            columns.put("userName", "用户名");
            columns.put("phone", "手机号");
            columns.put("amount", "实收金额");
            columns.put("status", "订单状态");
            columns.put("orderTime", "下单时间");

            List<Map<String, Object>> dataList = buildOrderDataList(orders, false);
            double totalAmount = orders.stream()
                    .filter(o -> o.getAmount() != null)
                    .mapToDouble(o -> o.getAmount().doubleValue())
                    .sum();

            Map<String, String> summary = new LinkedHashMap<>();
            summary.put("订单总数", String.valueOf(orders.size()));
            summary.put("总金额", "¥" + String.format("%.2f", totalAmount));
            summary.put("日期范围", (startDate != null ? startDate.toString() : "不限")
                    + " ~ " + (endDate != null ? endDate.toString() : "不限"));

            byte[] bytes = ExportUtil.generatePdfBytes("瑞吉外卖 - 订单数据报表", columns, dataList, summary);
            return buildFileResponse(bytes, "订单报表", "pdf");
        } catch (Exception e) {
            log.error("导出订单PDF失败: startDate={}, endDate={}", startDate, endDate, e);
            return buildErrorResponse("订单PDF导出失败，请稍后重试");
        }
    }

    // ==================== 菜品导出 ====================

    /**
     * 导出菜品数据 - Excel
     * 修改点：抽取buildDishDataList公共方法，Excel/PDF共用
     *
     * @param categoryId 分类ID（可选）
     * @return Excel文件流
     */
    @GetMapping("/dishes/excel")
    @Operation(summary = "导出菜品Excel", description = "导出菜品数据为Excel文件，支持按分类筛选")
    public ResponseEntity<?> exportDishesExcel(
                        @Parameter(description = "分类ID（可选）") @RequestParam(required = false) Long categoryId) {

        try {
            LinkedHashMap<String, String> columns = new LinkedHashMap<>();
            columns.put("name", "菜品名称");
            columns.put("categoryName", "分类");
            columns.put("price", "价格");
            columns.put("status", "状态");
            columns.put("description", "描述");
            columns.put("createTime", "创建时间");

            List<Map<String, Object>> dataList = buildDishDataList(categoryId);
            byte[] bytes = ExportUtil.generateExcelBytes(columns, dataList);
            return buildFileResponse(bytes, "菜品数据", "xlsx");
        } catch (Exception e) {
            log.error("导出菜品Excel失败: categoryId={}", categoryId, e);
            return buildErrorResponse("菜品Excel导出失败，请稍后重试");
        }
    }

    /**
     * 导出菜品数据 - PDF
     *
     * @param categoryId 分类ID（可选）
     * @return PDF文件流
     */
    @GetMapping("/dishes/pdf")
    @Operation(summary = "导出菜品PDF", description = "导出菜品数据为PDF报表，支持按分类筛选")
    public ResponseEntity<?> exportDishesPdf(
                        @Parameter(description = "分类ID（可选）") @RequestParam(required = false) Long categoryId) {

        try {
            LinkedHashMap<String, String> columns = new LinkedHashMap<>();
            columns.put("name", "菜品名称");
            columns.put("categoryName", "分类");
            columns.put("price", "价格");
            columns.put("status", "状态");
            columns.put("createTime", "创建时间");

            List<Map<String, Object>> dataList = buildDishDataList(categoryId);

            Map<String, String> summary = new LinkedHashMap<>();
            summary.put("菜品总数", String.valueOf(dataList.size()));

            byte[] bytes = ExportUtil.generatePdfBytes(
                    "瑞吉外卖 - 菜品数据报表", columns, dataList, summary);
            return buildFileResponse(bytes, "菜品报表", "pdf");
        } catch (Exception e) {
            log.error("导出菜品PDF失败: categoryId={}", categoryId, e);
            return buildErrorResponse("菜品PDF导出失败，请稍后重试");
        }
    }

    // ==================== 员工导出 ====================

    /**
     * 导出员工数据 - Excel
     *
     * @return Excel文件流
     */
    @GetMapping("/employees/excel")
    @Operation(summary = "导出员工Excel", description = "导出员工数据为Excel文件")
    public ResponseEntity<?> exportEmployeesExcel() {

        try {
            LinkedHashMap<String, String> columns = new LinkedHashMap<>();
            columns.put("name", "姓名");
            columns.put("username", "账号");
            columns.put("phone", "手机号");
            columns.put("sex", "性别");
            columns.put("status", "状态");
            columns.put("createTime", "入职时间");

            List<Map<String, Object>> dataList = buildEmployeeDataList(queryEmployees());
            byte[] bytes = ExportUtil.generateExcelBytes(columns, dataList);
            return buildFileResponse(bytes, "员工数据", "xlsx");
        } catch (Exception e) {
            log.error("导出员工Excel失败", e);
            return buildErrorResponse("员工Excel导出失败，请稍后重试");
        }
    }

    /**
     * 导出员工数据 - PDF
     *
     * @return PDF文件流
     */
    @GetMapping("/employees/pdf")
    @Operation(summary = "导出员工PDF", description = "导出员工数据为PDF报表")
    public ResponseEntity<?> exportEmployeesPdf() {

        try {
            LinkedHashMap<String, String> columns = new LinkedHashMap<>();
            columns.put("name", "姓名");
            columns.put("username", "账号");
            columns.put("phone", "手机号");
            columns.put("sex", "性别");
            columns.put("status", "状态");

            List<Employee> employees = queryEmployees();
            List<Map<String, Object>> dataList = buildEmployeeDataList(employees);

            Map<String, String> summary = new LinkedHashMap<>();
            summary.put("员工总数", String.valueOf(employees.size()));

            byte[] bytes = ExportUtil.generatePdfBytes(
                    "瑞吉外卖 - 员工数据报表", columns, dataList, summary);
            return buildFileResponse(bytes, "员工报表", "pdf");
        } catch (Exception e) {
            log.error("导出员工PDF失败", e);
            return buildErrorResponse("员工PDF导出失败，请稍后重试");
        }
    }

    // ==================== 私有数据查询方法 ====================

    /**
     * 查询订单列表
     */
    private List<Orders> queryOrders(LocalDate startDate, LocalDate endDate, Integer status) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        // #11 fail-closed：强制租户过滤，无租户上下文拒绝导出
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.reggie.common.CustomException("无导出权限，租户上下文缺失");
        }
        wrapper.eq(Orders::getTenantId, tenantId);
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
        // #12 限制最大导出行数，防止全量加载 OOM
        wrapper.last("LIMIT 100000");
        return orderService.list(wrapper);
    }

    /**
     * 查询菜品列表
     */
    private List<Dish> queryDishes(Long categoryId) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        // #11 fail-closed：强制租户过滤
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.reggie.common.CustomException("无导出权限，租户上下文缺失");
        }
        wrapper.eq(Dish::getTenantId, tenantId);
        wrapper.eq(Dish::getIsDeleted, 0);
        if (categoryId != null) {
            wrapper.eq(Dish::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Dish::getCreateTime);
        return dishService.list(wrapper);
    }

    /**
     * 查询员工列表
     */
    private List<Employee> queryEmployees() {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        addTenantFilter(wrapper);
        wrapper.orderByDesc(Employee::getCreateTime);
        return employeeService.list(wrapper);
    }

    // ==================== 数据构建方法 ====================

    /**
     * 构建订单数据列表
     * 修改点：Excel/PDF共用此方法，通过includeFull参数控制列数
     */
    private List<Map<String, Object>> buildOrderDataList(List<Orders> orders, boolean includeFull) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Orders order : orders) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("number", order.getNumber());
            row.put("userName", order.getUserName());
            row.put("phone", order.getPhone());
            if (includeFull) {
                row.put("address", order.getAddress());
            }
            row.put("amount", (order.getAmount() != null ? order.getAmount() : 0) + "元");
            row.put("status", getOrderStatusName(order.getStatus()));
            if (includeFull) {
                row.put("payMethod", order.getPayMethod() != null
                        ? (order.getPayMethod() == 1 ? "微信支付" : "支付宝") : "");
            }
            row.put("orderTime", order.getOrderTime());
            dataList.add(row);
        }
        return dataList;
    }

    /**
     * 构建菜品数据列表
     * 修改点：Excel/PDF共用，消除重复代码
     */
    private List<Map<String, Object>> buildDishDataList(Long categoryId) {
        List<Dish> dishes = queryDishes(categoryId);

        Map<Long, String> categoryMap = new HashMap<>();
        LambdaQueryWrapper<Category> categoryWrapper = new LambdaQueryWrapper<>();
        // #11 fail-closed：强制租户过滤
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.reggie.common.CustomException("无导出权限，租户上下文缺失");
        }
        categoryWrapper.eq(Category::getTenantId, tenantId);
        categoryWrapper.orderByAsc(Category::getSort);
        for (Category c : categoryService.list(categoryWrapper)) {
            categoryMap.put(c.getId(), c.getName());
        }

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Dish dish : dishes) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", dish.getName());
            row.put("categoryName", categoryMap.getOrDefault(dish.getCategoryId(), "未知"));
            row.put("price", (dish.getPrice() != null ? dish.getPrice() : 0) + "元");
            row.put("status", dish.getStatus() == 1 ? "在售" : "停售");
            row.put("description", dish.getDescription() != null ? dish.getDescription() : "");
            row.put("createTime", dish.getCreateTime());
            dataList.add(row);
        }
        return dataList;
    }

    /**
     * 构建员工数据列表
     */
    private List<Map<String, Object>> buildEmployeeDataList(List<Employee> employees) {
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
        return dataList;
    }

    // ==================== 工具方法 ====================

    /**
     * 订单状态中文名（委托给 {@link OrderStatus} 枚举）
     */
    private String getOrderStatusName(Integer status) {
        if (status == null) return "未知";
        OrderStatus orderStatus = OrderStatus.fromCode(status);
        return orderStatus != null ? orderStatus.getDesc() : "其他";
    }

    /**
     * 为employee查询手动添加tenant_id过滤
     * employee表在MybatisPlusConfig忽略列表中，必须手动隔离
     * #11 fail-closed：无租户上下文直接抛异常拒绝导出
     */
    private void addTenantFilter(LambdaQueryWrapper<Employee> wrapper) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.reggie.common.CustomException("无导出权限，租户上下文缺失");
        }
        wrapper.eq(Employee::getTenantId, tenantId);
    }

    // ==================== 响应构建方法 ====================

    /**
     * 构建文件下载响应
     * 修改点：统一设置Content-Type和Content-Disposition，确保前端正确识别
     */
    private ResponseEntity<byte[]> buildFileResponse(byte[] bytes, String fileName, String ext) {
        String fullName = fileName + "_" + LocalDateTime.now().format(FILE_DATE_FMT) + "." + ext;
        MediaType mediaType = "pdf".equals(ext)
                ? MediaType.parseMediaType("application/pdf")
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLength(bytes.length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fullName).build());

        log.info("文件下载响应构建完成: {} ({} bytes)", fullName, bytes.length);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /**
     * 构建错误响应
     * 修改点：返回JSON格式的错误信息，前端export.js能够正确解析并显示Toast
     */
    private ResponseEntity<R<String>> buildErrorResponse(String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(R.error(message), headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}







