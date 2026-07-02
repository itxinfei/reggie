## Task 2: 打印管理页面 + PrinterLogController

### PrinterLogController.java

```java
package com.reggie.module.printer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.printer.model.PrinterLog;
import com.reggie.module.printer.service.PrinterLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/printer/log")
@Slf4j
public class PrinterLogController {

    @Autowired
    private PrinterLogService printerLogService;

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, Long orderId) {
        Page<PrinterLog> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<PrinterLog> qw = new LambdaQueryWrapper<>();
        qw.eq(orderId != null, PrinterLog::getOrderId, orderId);
        qw.orderByDesc(PrinterLog::getCreatedTime);
        printerLogService.page(pageInfo, qw);
        return R.success(pageInfo);
    }
}
```

### config-list.html 关键差异

- 表格列：name, brand, type, ipAddress, port, paperSize, printType, status(启用/停用标签)
- 表单字段：name, type(下拉:USB/TCP/CLOUD/BLUETOOTH), brand, deviceId, ipAddress, port, paperSize(下拉:58mm/80mm), printType(多选框), sort, status
- 操作列额外按钮：测试连接（调用 `printerTest(id)`）
- 引用的 API 文件：`../../api/printer.js`

### log-list.html 关键差异

- 纯展示页，无新增/编辑/删除操作
- 表格列：orderId, printType, printerId(显示名称), status(成功/失败标签), errorMsg, createdTime
- 搜索：orderId
- 引用的 API 文件：`../../api/printer.js`

### Steps

- [ ] **Step 1: 创建 `PrinterLogController.java`**
- [ ] **Step 2: 创建 `printer/config-list.html`**
- [ ] **Step 3: 创建 `printer/log-list.html`**
- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/reggie/module/printer/controller/PrinterLogController.java src/main/resources/backend/page/printer/config-list.html src/main/resources/backend/page/printer/log-list.html
git commit -m "feat(frontend): add printer management pages and PrinterLogController"
```
