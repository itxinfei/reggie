# 修复 M4: OrderServiceImpl N+1 查询优化

**Files:**
- Modify: `src/main/java/com/reggie/service/impl/OrderServiceImpl.java`

## 问题描述

OrderServiceImpl.userPage() 方法中，使用 stream().map() 内嵌 filter()，导致每次遍历都扫描整个 details 列表。

## 当前代码（第140-152行）

```java
List<OrderDto> orderDtoList = pageInfo.getRecords().stream().map(order -> {
    OrderDto dto = new OrderDto();
    BeanUtils.copyProperties(order, dto);
    dto.setOrderDetails(details.stream()
        .filter(d -> d.getOrderId().equals(order.getId())) // 每次都遍历整个details
        .collect(Collectors.toList()));
    return dto;
}).collect(Collectors.toList());
```

## 修复方案

使用 Map 预分组，将 O(n²) 降为 O(n)：

```java
// 预构建 Map<orderId, List<OrderDetail>>
Map<Long, List<OrderDetail>> detailsMap = details.stream()
    .collect(Collectors.groupingBy(OrderDetail::getOrderId));

List<OrderDto> orderDtoList = pageInfo.getRecords().stream().map(order -> {
    OrderDto dto = new OrderDto();
    BeanUtils.copyProperties(order, dto);
    dto.setOrderDetails(detailsMap.getOrDefault(order.getId(), Collections.emptyList()));
    return dto;
}).collect(Collectors.toList());
```

## 具体步骤

1. 在 `details` 列表获取后，添加 Map 分组逻辑
2. 替换原有的 stream().map() + filter() 为 Map.getOrDefault()
3. 添加 import：`import java.util.Collections;`

## 验收标准

- [ ] 使用 Map 分组优化
- [ ] 时间复杂度从 O(n²) 降为 O(n)
- [ ] 添加必要的 import
- [ ] 编译通过
- [ ] 所有现有测试通过（OrderControllerTest: 3 PASSED）
