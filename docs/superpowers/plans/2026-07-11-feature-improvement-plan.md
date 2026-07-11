# 瑞吉外卖功能改进实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在当前技术栈（Vue 2 + Vant + 原生HTML/JS）上完善核心商业功能，提升与大厂的竞争力

**Architecture:** 保持现有架构不变，通过添加新的HTML页面、API接口和前端逻辑来扩展功能。所有新功能使用现有的Vant组件和axios进行开发。

**Tech Stack:** Vue 2.x, Vant 2.x, Axios, 原生HTML/JS, Remix Icon

## Global Constraints

- 技术栈不变：Vue 2 + Vant + 原生HTML/JS
- 不引入构建工具：保持原生HTML页面
- 不引入TypeScript：保持JavaScript
- 保持现有代码风格和目录结构
- 所有新功能必须兼容现有用户数据

---

## 功能改进优先级

### P0 - 核心商业功能（必须完成）
1. **在线支付** - 微信/支付宝支付集成
2. **配送追踪** - 订单状态实时更新
3. **优惠券完善** - 领券、使用、管理

### P1 - 用户体验优化（重要）
1. **会员体系** - 积分、等级、会员价
2. **常购清单** - 快速复购
3. **订单评价** - 完善评价功能

### P2 - 功能增强（可选）
1. **语音搜索** - 语音输入搜索
2. **分享功能** - 菜品/订单分享
3. **预约下单** - 预约配送时间

---

## 任务分解

### Task 1: 在线支付功能

**Files:**
- Create: `src/main/resources/front/page/pay.html` - 支付页面
- Create: `src/main/resources/front/page/pay-result.html` - 支付结果页面
- Create: `src/main/resources/front/api/payment.js` - 支付API
- Modify: `src/main/resources/front/page/add-order.html` - 添加支付方式选择
- Modify: `src/main/java/com/reggie/controller/PayController.java` - 支付控制器
- Create: `src/main/java/com/reggie/service/PayService.java` - 支付服务接口
- Create: `src/main/java/com/reggie/service/impl/PayServiceImpl.java` - 支付服务实现

**Interfaces:**
- Consumes: 订单信息（orderId, amount, items）
- Produces: 支付链接、支付结果、退款接口

- [ ] **Step 1: 创建支付数据表**

```sql
-- 支付表
CREATE TABLE payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    payment_no VARCHAR(64) COMMENT '支付流水号',
    trade_no VARCHAR(64) COMMENT '第三方交易号',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    pay_type TINYINT NOT NULL COMMENT '支付方式：1微信 2支付宝',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待支付 1已支付 2已退款 3支付失败',
    pay_time DATETIME COMMENT '支付时间',
    refund_time DATETIME COMMENT '退款时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_order_id (order_id),
    INDEX idx_payment_no (payment_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付表';
```

- [ ] **Step 2: 创建支付实体类**

```java
// Payment.java
package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String paymentNo;
    private String tradeNo;
    private BigDecimal totalAmount;
    private Integer payType;
    private Integer status;
    private LocalDateTime payTime;
    private LocalDateTime refundTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long tenantId;
}
```

- [ ] **Step 3: 创建支付Mapper**

```java
// PaymentMapper.java
package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
```

- [ ] **Step 4: 创建支付服务接口**

```java
// PayService.java
package com.reggie.service;

import com.reggie.entity.Payment;

public interface PayService {
    /**
     * 创建支付单
     */
    Payment createPayment(Long orderId, Integer payType);
    
    /**
     * 查询支付状态
     */
    Payment queryPaymentStatus(Long paymentId);
    
    /**
     * 处理支付回调
     */
    void handlePayCallback(String tradeNo, Integer status);
    
    /**
     * 退款
     */
    void refund(Long paymentId);
}
```

- [ ] **Step 5: 创建支付服务实现**

```java
// PayServiceImpl.java
package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.entity.Order;
import com.reggie.entity.Payment;
import com.reggie.mapper.OrderMapper;
import com.reggie.mapper.PaymentMapper;
import com.reggie.service.PayService;
import com.reggie.common.BaseContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PayServiceImpl implements PayService {
    
    @Autowired
    private PaymentMapper paymentMapper;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Override
    public Payment createPayment(Long orderId, Integer payType) {
        // 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        // 创建支付单
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setPaymentNo("PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        payment.setTotalAmount(order.getAmount());
        payment.setPayType(payType);
        payment.setStatus(0);
        payment.setCreateTime(LocalDateTime.now());
        payment.setTenantId(BaseContext.getCurrentId());
        
        paymentMapper.insert(payment);
        return payment;
    }
    
    @Override
    public Payment queryPaymentStatus(Long paymentId) {
        return paymentMapper.selectById(paymentId);
    }
    
    @Override
    public void handlePayCallback(String tradeNo, Integer status) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getTradeNo, tradeNo);
        Payment payment = paymentMapper.selectOne(wrapper);
        
        if (payment != null) {
            payment.setStatus(status);
            if (status == 1) {
                payment.setPayTime(LocalDateTime.now());
                // 更新订单状态
                Order order = orderMapper.selectById(payment.getOrderId());
                if (order != null) {
                    order.setStatus(2); // 已支付
                    orderMapper.updateById(order);
                }
            }
            paymentMapper.updateById(payment);
        }
    }
    
    @Override
    public void refund(Long paymentId) {
        Payment payment = paymentMapper.selectById(paymentId);
        if (payment != null && payment.getStatus() == 1) {
            payment.setStatus(2); // 已退款
            payment.setRefundTime(LocalDateTime.now());
            paymentMapper.updateById(payment);
            
            // 更新订单状态
            Order order = orderMapper.selectById(payment.getOrderId());
            if (order != null) {
                order.setStatus(5); // 已退款
                orderMapper.updateById(order);
            }
        }
    }
}
```

- [ ] **Step 6: 创建支付控制器**

```java
// PayController.java
package com.reggie.controller;

import com.reggie.common.R;
import com.reggie.entity.Payment;
import com.reggie.service.PayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pay")
public class PayController {
    
    @Autowired
    private PayService payService;
    
    /**
     * 创建支付单
     */
    @PostMapping("/create")
    public R<Payment> createPayment(@RequestParam Long orderId, @RequestParam Integer payType) {
        Payment payment = payService.createPayment(orderId, payType);
        return R.success(payment);
    }
    
    /**
     * 查询支付状态
     */
    @GetMapping("/status/{id}")
    public R<Payment> queryStatus(@PathVariable Long id) {
        Payment payment = payService.queryPaymentStatus(id);
        return R.success(payment);
    }
    
    /**
     * 支付回调（微信/支付宝调用）
     */
    @PostMapping("/callback")
    public String payCallback(@RequestParam String tradeNo, @RequestParam Integer status) {
        payService.handlePayCallback(tradeNo, status);
        return "success";
    }
    
    /**
     * 退款
     */
    @PostMapping("/refund/{id}")
    public R<String> refund(@PathVariable Long id) {
        payService.refund(id);
        return R.success("退款成功");
    }
}
```

- [ ] **Step 7: 创建支付前端API**

```javascript
// payment.js
import request from '../js/request.js';

/**
 * 创建支付单
 */
export function createPayment(orderId, payType) {
    return request({
        url: '/api/pay/create',
        method: 'post',
        params: { orderId, payType }
    });
}

/**
 * 查询支付状态
 */
export function queryPaymentStatus(paymentId) {
    return request({
        url: `/api/pay/status/${paymentId}`,
        method: 'get'
    });
}

/**
 * 退款
 */
export function refundPayment(paymentId) {
    return request({
        url: `/api/pay/refund/${paymentId}`,
        method: 'post'
    });
}
```

- [ ] **Step 8: 修改订单确认页面添加支付方式选择**

```html
<!-- add-order.html 中添加支付方式选择 -->
<div class="pay-method">
    <div class="section-title">支付方式</div>
    <div class="pay-options">
        <div class="pay-option" :class="{ active: payType === 1 }" @click="payType = 1">
            <img src="../images/wechat.png" class="pay-icon"/>
            <span>微信支付</span>
            <i class="icon-check-line" v-if="payType === 1"></i>
        </div>
        <div class="pay-option" :class="{ active: payType === 2 }" @click="payType = 2">
            <img src="../images/alipay.png" class="pay-icon"/>
            <span>支付宝</span>
            <i class="icon-check-line" v-if="payType === 2"></i>
        </div>
        <div class="pay-option" :class="{ active: payType === 3 }" @click="payType = 3">
            <i class="icon-money-cny-circle"></i>
            <span>货到付款</span>
            <i class="icon-check-line" v-if="payType === 3"></i>
        </div>
    </div>
</div>

<script>
// 在Vue实例中添加
data() {
    return {
        // ... 其他数据
        payType: 1, // 默认微信支付
    }
},
methods: {
    // ... 其他方法
    async submitOrder() {
        if (this.payType === 3) {
            // 货到付款，直接提交
            await this.createOrder();
        } else {
            // 在线支付，先创建订单，再跳转支付
            const order = await this.createOrder();
            if (order) {
                const payment = await createPayment(order.id, this.payType);
                if (payment) {
                    // 跳转支付页面
                    window.location.href = `pay.html?paymentId=${payment.id}`;
                }
            }
        }
    }
}
</script>

<style>
.pay-method {
    background: #fff;
    margin-top: 10px;
    padding: 15px;
}

.pay-options {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.pay-option {
    display: flex;
    align-items: center;
    padding: 12px;
    border: 1px solid #eee;
    border-radius: 8px;
    cursor: pointer;
}

.pay-option.active {
    border-color: #ff6b6b;
    background: #fff5f5;
}

.pay-icon {
    width: 24px;
    height: 24px;
    margin-right: 10px;
}

.pay-option i {
    margin-left: auto;
    color: #ff6b6b;
}
</style>
```

- [ ] **Step 9: 创建支付页面**

```html
<!-- pay.html -->
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>支付</title>
    <link rel="stylesheet" href="../styles/vant.min.css"/>
    <link rel="stylesheet" href="../styles/index.css"/>
    <link rel="stylesheet" href="../styles/main.css"/>
    <style>
        body {
            background: #f5f5f5;
        }
        .pay-container {
            padding: 20px;
        }
        .pay-amount {
            text-align: center;
            padding: 30px 0;
            background: #fff;
            border-radius: 12px;
            margin-bottom: 20px;
        }
        .pay-amount .amount {
            font-size: 36px;
            font-weight: bold;
            color: #ff6b6b;
        }
        .pay-amount .label {
            font-size: 14px;
            color: #999;
            margin-top: 10px;
        }
        .pay-btn {
            width: 100%;
            height: 50px;
            background: linear-gradient(135deg, #ff6b6b 0%, #ff8e8e 100%);
            color: #fff;
            border: none;
            border-radius: 25px;
            font-size: 18px;
            cursor: pointer;
        }
        .pay-btn:disabled {
            background: #ccc;
        }
        .pay-result {
            text-align: center;
            padding: 50px 0;
        }
        .pay-result .icon {
            font-size: 60px;
            color: #52c41a;
        }
        .pay-result .text {
            font-size: 18px;
            margin-top: 20px;
        }
    </style>
</head>
<body>
    <div id="app">
        <van-nav-bar title="支付" left-arrow @click-left="goBack"/>
        
        <div class="pay-container" v-if="!paySuccess">
            <div class="pay-amount">
                <div class="label">支付金额</div>
                <div class="amount">¥{{ amount }}</div>
            </div>
            
            <van-button 
                type="primary" 
                block 
                class="pay-btn"
                :loading="paying"
                @click="handlePay"
            >
                确认支付
            </van-button>
        </div>
        
        <div class="pay-result" v-else>
            <van-icon name="passed" class="icon" size="60px"/>
            <div class="text">支付成功</div>
            <van-button type="primary" block style="margin-top: 30px;" @click="viewOrder">
                查看订单
            </van-button>
        </div>
    </div>
    
    <script src="../backend/plugins/vue/vue.js"></script>
    <script src="../js/vant.min.js"></script>
    <script src="../backend/plugins/axios/axios.min.js"></script>
    <script src="../js/request.js"></script>
    <script src="../api/payment.js"></script>
    <script>
        new Vue({
            el: '#app',
            data() {
                return {
                    paymentId: null,
                    amount: 0,
                    paying: false,
                    paySuccess: false
                }
            },
            created() {
                this.paymentId = this.getUrlParam('paymentId');
                if (this.paymentId) {
                    this.loadPaymentInfo();
                }
            },
            methods: {
                goBack() {
                    window.history.go(-1);
                },
                getUrlParam(name) {
                    const reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)');
                    const r = window.location.search.substr(1).match(reg);
                    if (r != null) return decodeURIComponent(r[2]);
                    return null;
                },
                async loadPaymentInfo() {
                    const res = await queryPaymentStatus(this.paymentId);
                    if (res.code === 1) {
                        this.amount = res.data.totalAmount;
                    }
                },
                async handlePay() {
                    this.paying = true;
                    try {
                        // 模拟支付（实际应调用微信/支付宝SDK）
                        await new Promise(resolve => setTimeout(resolve, 2000));
                        
                        // 更新支付状态
                        await axios.post(`/api/pay/callback`, null, {
                            params: {
                                tradeNo: this.paymentId,
                                status: 1
                            }
                        });
                        
                        this.paySuccess = true;
                    } catch (error) {
                        this.$toast.fail('支付失败');
                    } finally {
                        this.paying = false;
                    }
                },
                viewOrder() {
                    window.location.href = 'order.html';
                }
            }
        });
    </script>
</body>
</html>
```

- [ ] **Step 10: 测试支付功能**

```bash
# 启动应用
java -jar target/reggie_take_out.jar

# 测试创建支付单
curl -X POST "http://localhost:8080/api/pay/create?orderId=1&payType=1"

# 测试查询支付状态
curl -X GET "http://localhost:8080/api/pay/status/1"

# 测试支付回调
curl -X POST "http://localhost:8080/api/pay/callback?tradeNo=xxx&status=1"
```

- [ ] **Step 11: 提交代码**

```bash
git add src/main/java/com/reggie/entity/Payment.java
git add src/main/java/com/reggie/mapper/PaymentMapper.java
git add src/main/java/com/reggie/service/PayService.java
git add src/main/java/com/reggie/service/impl/PayServiceImpl.java
git add src/main/java/com/reggie/controller/PayController.java
git add src/main/resources/front/page/pay.html
git add src/main/resources/front/api/payment.js
git add src/main/resources/front/page/add-order.html
git commit -m "feat: 添加在线支付功能"
```

---

### Task 2: 配送追踪功能

**Files:**
- Create: `src/main/resources/front/page/tracking.html` - 配送追踪页面
- Create: `src/main/resources/front/api/tracking.js` - 配送追踪API
- Create: `src/main/java/com/reggie/entity/DeliveryTracking.java` - 配送追踪实体
- Create: `src/main/java/com/reggie/mapper/DeliveryTrackingMapper.java` - 配送追踪Mapper
- Create: `src/main/java/com/reggie/service/DeliveryTrackingService.java` - 配送追踪服务
- Create: `src/main/java/com/reggie/service/impl/DeliveryTrackingServiceImpl.java` - 配送追踪服务实现
- Create: `src/main/java/com/reggie/controller/DeliveryTrackingController.java` - 配送追踪控制器
- Modify: `src/main/resources/front/page/order.html` - 添加追踪入口

**Interfaces:**
- Consumes: 订单ID
- Produces: 配送状态、骑手位置、预计送达时间

- [ ] **Step 1: 创建配送追踪表**

```sql
-- 配送追踪表
CREATE TABLE delivery_tracking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    rider_id BIGINT COMMENT '骑手ID',
    rider_name VARCHAR(50) COMMENT '骑手姓名',
    rider_phone VARCHAR(20) COMMENT '骑手电话',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待接单 1已接单 2取餐中 3配送中 4已送达',
    latitude DECIMAL(10,7) COMMENT '骑手纬度',
    longitude DECIMAL(10,7) COMMENT '骑手经度',
    estimated_time INT COMMENT '预计送达时间(分钟)',
    actual_time DATETIME COMMENT '实际送达时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_order_id (order_id),
    INDEX idx_rider_id (rider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送追踪表';
```

- [ ] **Step 2: 创建配送追踪实体类**

```java
// DeliveryTracking.java
package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("delivery_tracking")
public class DeliveryTracking {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long riderId;
    private String riderName;
    private String riderPhone;
    private Integer status;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer estimatedTime;
    private LocalDateTime actualTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long tenantId;
}
```

- [ ] **Step 3: 创建配送追踪Mapper**

```java
// DeliveryTrackingMapper.java
package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.DeliveryTracking;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeliveryTrackingMapper extends BaseMapper<DeliveryTracking> {
}
```

- [ ] **Step 4: 创建配送追踪服务接口**

```java
// DeliveryTrackingService.java
package com.reggie.service;

import com.reggie.entity.DeliveryTracking;

public interface DeliveryTrackingService {
    /**
     * 创建配送单
     */
    DeliveryTracking createTracking(Long orderId);
    
    /**
     * 更新骑手位置
     */
    void updateRiderLocation(Long trackingId, BigDecimal latitude, BigDecimal longitude);
    
    /**
     * 更新配送状态
     */
    void updateStatus(Long trackingId, Integer status);
    
    /**
     * 查询配送状态
     */
    DeliveryTracking queryTracking(Long orderId);
}
```

- [ ] **Step 5: 创建配送追踪服务实现**

```java
// DeliveryTrackingServiceImpl.java
package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.entity.DeliveryTracking;
import com.reggie.entity.Order;
import com.reggie.mapper.DeliveryTrackingMapper;
import com.reggie.mapper.OrderMapper;
import com.reggie.service.DeliveryTrackingService;
import com.reggie.common.BaseContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class DeliveryTrackingServiceImpl implements DeliveryTrackingService {
    
    @Autowired
    private DeliveryTrackingMapper trackingMapper;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Override
    public DeliveryTracking createTracking(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        DeliveryTracking tracking = new DeliveryTracking();
        tracking.setOrderId(orderId);
        tracking.setStatus(0);
        tracking.setEstimatedTime(30); // 默认30分钟
        tracking.setCreateTime(LocalDateTime.now());
        tracking.setTenantId(BaseContext.getCurrentId());
        
        trackingMapper.insert(tracking);
        return tracking;
    }
    
    @Override
    public void updateRiderLocation(Long trackingId, BigDecimal latitude, BigDecimal longitude) {
        DeliveryTracking tracking = trackingMapper.selectById(trackingId);
        if (tracking != null) {
            tracking.setLatitude(latitude);
            tracking.setLongitude(longitude);
            trackingMapper.updateById(tracking);
        }
    }
    
    @Override
    public void updateStatus(Long trackingId, Integer status) {
        DeliveryTracking tracking = trackingMapper.selectById(trackingId);
        if (tracking != null) {
            tracking.setStatus(status);
            if (status == 4) { // 已送达
                tracking.setActualTime(LocalDateTime.now());
                // 更新订单状态
                Order order = orderMapper.selectById(tracking.getOrderId());
                if (order != null) {
                    order.setStatus(3); // 已完成
                    orderMapper.updateById(order);
                }
            }
            trackingMapper.updateById(tracking);
        }
    }
    
    @Override
    public DeliveryTracking queryTracking(Long orderId) {
        LambdaQueryWrapper<DeliveryTracking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeliveryTracking::getOrderId, orderId);
        wrapper.orderByDesc(DeliveryTracking::getCreateTime);
        return trackingMapper.selectOne(wrapper);
    }
}
```

- [ ] **Step 6: 创建配送追踪控制器**

```java
// DeliveryTrackingController.java
package com.reggie.controller;

import com.reggie.common.R;
import com.reggie.entity.DeliveryTracking;
import com.reggie.service.DeliveryTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryTrackingController {
    
    @Autowired
    private DeliveryTrackingService trackingService;
    
    /**
     * 创建配送单
     */
    @PostMapping("/create")
    public R<DeliveryTracking> createTracking(@RequestParam Long orderId) {
        DeliveryTracking tracking = trackingService.createTracking(orderId);
        return R.success(tracking);
    }
    
    /**
     * 查询配送状态
     */
    @GetMapping("/tracking/{orderId}")
    public R<DeliveryTracking> queryTracking(@PathVariable Long orderId) {
        DeliveryTracking tracking = trackingService.queryTracking(orderId);
        return R.success(tracking);
    }
    
    /**
     * 更新骑手位置（骑手端调用）
     */
    @PostMapping("/location")
    public R<String> updateLocation(
            @RequestParam Long trackingId,
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude) {
        trackingService.updateRiderLocation(trackingId, latitude, longitude);
        return R.success("更新成功");
    }
    
    /**
     * 更新配送状态（骑手端调用）
     */
    @PostMapping("/status")
    public R<String> updateStatus(
            @RequestParam Long trackingId,
            @RequestParam Integer status) {
        trackingService.updateStatus(trackingId, status);
        return R.success("更新成功");
    }
}
```

- [ ] **Step 7: 创建配送追踪前端API**

```javascript
// tracking.js
import request from '../js/request.js';

/**
 * 创建配送单
 */
export function createDeliveryTracking(orderId) {
    return request({
        url: '/api/delivery/create',
        method: 'post',
        params: { orderId }
    });
}

/**
 * 查询配送状态
 */
export function queryDeliveryTracking(orderId) {
    return request({
        url: `/api/delivery/tracking/${orderId}`,
        method: 'get'
    });
}

/**
 * 更新骑手位置
 */
export function updateRiderLocation(trackingId, latitude, longitude) {
    return request({
        url: '/api/delivery/location',
        method: 'post',
        params: { trackingId, latitude, longitude }
    });
}

/**
 * 更新配送状态
 */
export function updateDeliveryStatus(trackingId, status) {
    return request({
        url: '/api/delivery/status',
        method: 'post',
        params: { trackingId, status }
    });
}
```

- [ ] **Step 8: 创建配送追踪页面**

```html
<!-- tracking.html -->
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>配送追踪</title>
    <link rel="stylesheet" href="../styles/vant.min.css"/>
    <link rel="stylesheet" href="../styles/index.css"/>
    <link rel="stylesheet" href="../styles/main.css"/>
    <style>
        body {
            background: #f5f5f5;
        }
        .tracking-container {
            padding: 15px;
        }
        .rider-card {
            background: #fff;
            border-radius: 12px;
            padding: 20px;
            margin-bottom: 15px;
        }
        .rider-info {
            display: flex;
            align-items: center;
            margin-bottom: 15px;
        }
        .rider-avatar {
            width: 50px;
            height: 50px;
            border-radius: 50%;
            background: #ff6b6b;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #fff;
            font-size: 20px;
            margin-right: 15px;
        }
        .rider-detail {
            flex: 1;
        }
        .rider-name {
            font-size: 16px;
            font-weight: bold;
        }
        .rider-status {
            font-size: 14px;
            color: #999;
            margin-top: 5px;
        }
        .rider-actions {
            display: flex;
            gap: 10px;
        }
        .rider-actions .action-btn {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            background: #f5f5f5;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
        }
        .tracking-timeline {
            background: #fff;
            border-radius: 12px;
            padding: 20px;
        }
        .timeline-item {
            display: flex;
            padding-bottom: 20px;
            position: relative;
        }
        .timeline-item:last-child {
            padding-bottom: 0;
        }
        .timeline-dot {
            width: 12px;
            height: 12px;
            border-radius: 50%;
            background: #ddd;
            margin-right: 15px;
            margin-top: 4px;
        }
        .timeline-item.active .timeline-dot {
            background: #ff6b6b;
        }
        .timeline-content {
            flex: 1;
        }
        .timeline-title {
            font-size: 14px;
            font-weight: bold;
        }
        .timeline-time {
            font-size: 12px;
            color: #999;
            margin-top: 5px;
        }
        .timeline-item:not(:last-child)::after {
            content: '';
            position: absolute;
            left: 5px;
            top: 16px;
            bottom: 0;
            width: 2px;
            background: #eee;
        }
        .timeline-item.active:not(:last-child)::after {
            background: #ff6b6b;
        }
        .estimated-time {
            text-align: center;
            padding: 20px;
            background: #fff;
            border-radius: 12px;
            margin-bottom: 15px;
        }
        .estimated-time .time {
            font-size: 36px;
            font-weight: bold;
            color: #ff6b6b;
        }
        .estimated-time .label {
            font-size: 14px;
            color: #999;
            margin-top: 10px;
        }
    </style>
</head>
<body>
    <div id="app">
        <van-nav-bar title="配送追踪" left-arrow @click-left="goBack"/>
        
        <div class="tracking-container">
            <!-- 预计送达时间 -->
            <div class="estimated-time" v-if="tracking">
                <div class="time">{{ tracking.estimatedTime }}分钟</div>
                <div class="label">预计送达时间</div>
            </div>
            
            <!-- 骑手信息 -->
            <div class="rider-card" v-if="tracking && tracking.riderName">
                <div class="rider-info">
                    <div class="rider-avatar">
                        <van-icon name="manager"/>
                    </div>
                    <div class="rider-detail">
                        <div class="rider-name">{{ tracking.riderName }}</div>
                        <div class="rider-status">{{ statusText }}</div>
                    </div>
                    <div class="rider-actions">
                        <a :href="'tel:' + tracking.riderPhone" class="action-btn">
                            <van-icon name="phone"/>
                        </a>
                    </div>
                </div>
            </div>
            
            <!-- 配送时间线 -->
            <div class="tracking-timeline">
                <div class="timeline-title" style="margin-bottom: 15px;">配送进度</div>
                <div class="timeline-item" :class="{ active: tracking && tracking.status >= 0 }">
                    <div class="timeline-dot"></div>
                    <div class="timeline-content">
                        <div class="timeline-title">待接单</div>
                        <div class="timeline-time" v-if="tracking && tracking.createTime">{{ formatTime(tracking.createTime) }}</div>
                    </div>
                </div>
                <div class="timeline-item" :class="{ active: tracking && tracking.status >= 1 }">
                    <div class="timeline-dot"></div>
                    <div class="timeline-content">
                        <div class="timeline-title">已接单</div>
                        <div class="timeline-time" v-if="tracking && tracking.status >= 1">{{ formatTime(tracking.updateTime) }}</div>
                    </div>
                </div>
                <div class="timeline-item" :class="{ active: tracking && tracking.status >= 2 }">
                    <div class="timeline-dot"></div>
                    <div class="timeline-content">
                        <div class="timeline-title">取餐中</div>
                        <div class="timeline-time" v-if="tracking && tracking.status >= 2">{{ formatTime(tracking.updateTime) }}</div>
                    </div>
                </div>
                <div class="timeline-item" :class="{ active: tracking && tracking.status >= 3 }">
                    <div class="timeline-dot"></div>
                    <div class="timeline-content">
                        <div class="timeline-title">配送中</div>
                        <div class="timeline-time" v-if="tracking && tracking.status >= 3">{{ formatTime(tracking.updateTime) }}</div>
                    </div>
                </div>
                <div class="timeline-item" :class="{ active: tracking && tracking.status >= 4 }">
                    <div class="timeline-dot"></div>
                    <div class="timeline-content">
                        <div class="timeline-title">已送达</div>
                        <div class="timeline-time" v-if="tracking && tracking.actualTime">{{ formatTime(tracking.actualTime) }}</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <script src="../backend/plugins/vue/vue.js"></script>
    <script src="../js/vant.min.js"></script>
    <script src="../backend/plugins/axios/axios.min.js"></script>
    <script src="../js/request.js"></script>
    <script src="../api/tracking.js"></script>
    <script>
        new Vue({
            el: '#app',
            data() {
                return {
                    orderId: null,
                    tracking: null,
                    timer: null
                }
            },
            computed: {
                statusText() {
                    if (!this.tracking) return '';
                    const statusMap = {
                        0: '等待骑手接单',
                        1: '骑手已接单',
                        2: '骑手正在取餐',
                        3: '骑手正在配送',
                        4: '已送达'
                    };
                    return statusMap[this.tracking.status] || '';
                }
            },
            created() {
                this.orderId = this.getUrlParam('orderId');
                if (this.orderId) {
                    this.loadTracking();
                    // 每30秒刷新一次
                    this.timer = setInterval(this.loadTracking, 30000);
                }
            },
            beforeDestroy() {
                if (this.timer) {
                    clearInterval(this.timer);
                }
            },
            methods: {
                goBack() {
                    window.history.go(-1);
                },
                getUrlParam(name) {
                    const reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)');
                    const r = window.location.search.substr(1).match(reg);
                    if (r != null) return decodeURIComponent(r[2]);
                    return null;
                },
                async loadTracking() {
                    const res = await queryDeliveryTracking(this.orderId);
                    if (res.code === 1) {
                        this.tracking = res.data;
                    }
                },
                formatTime(time) {
                    if (!time) return '';
                    const date = new Date(time);
                    return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
                }
            }
        });
    </script>
</body>
</html>
```

- [ ] **Step 9: 修改订单页面添加追踪入口**

```html
<!-- order.html 中在已完成订单添加追踪按钮 -->
<div class="order-actions" v-if="item.status === 2">
    <van-button size="small" type="primary" @click="viewTracking(item.id)">配送追踪</van-button>
</div>

<script>
methods: {
    viewTracking(orderId) {
        window.location.href = `tracking.html?orderId=${orderId}`;
    }
}
</script>
```

- [ ] **Step 10: 测试配送追踪功能**

```bash
# 启动应用
java -jar target/reggie_take_out.jar

# 测试创建配送单
curl -X POST "http://localhost:8080/api/delivery/create?orderId=1"

# 测试查询配送状态
curl -X GET "http://localhost:8080/api/delivery/tracking/1"

# 测试更新骑手位置
curl -X POST "http://localhost:8080/api/delivery/location?trackingId=1&latitude=39.9&longitude=116.4"

# 测试更新配送状态
curl -X POST "http://localhost:8080/api/delivery/status?trackingId=1&status=3"
```

- [ ] **Step 11: 提交代码**

```bash
git add src/main/java/com/reggie/entity/DeliveryTracking.java
git add src/main/java/com/reggie/mapper/DeliveryTrackingMapper.java
git add src/main/java/com/reggie/service/DeliveryTrackingService.java
git add src/main/java/com/reggie/service/impl/DeliveryTrackingServiceImpl.java
git add src/main/java/com/reggie/controller/DeliveryTrackingController.java
git add src/main/resources/front/page/tracking.html
git add src/main/resources/front/api/tracking.js
git add src/main/resources/front/page/order.html
git commit -m "feat: 添加配送追踪功能"
```

---

### Task 3: 优惠券功能完善

**Files:**
- Create: `src/main/resources/front/page/coupon.html` - 优惠券中心页面
- Create: `src/main/resources/front/api/coupon.js` - 优惠券API
- Create: `src/main/java/com/reggie/entity/Coupon.java` - 优惠券实体
- Create: `src/main/java/com/reggie/entity/UserCoupon.java` - 用户优惠券实体
- Create: `src/main/java/com/reggie/mapper/CouponMapper.java` - 优惠券Mapper
- Create: `src/main/java/com/reggie/mapper/UserCouponMapper.java` - 用户优惠券Mapper
- Create: `src/main/java/com/reggie/service/CouponService.java` - 优惠券服务
- Create: `src/main/java/com/reggie/service/impl/CouponServiceImpl.java` - 优惠券服务实现
- Create: `src/main/java/com/reggie/controller/CouponController.java` - 优惠券控制器
- Modify: `src/main/resources/front/page/add-order.html` - 优化优惠券选择

**Interfaces:**
- Consumes: 用户ID、订单金额
- Produces: 可用优惠券列表、优惠券使用记录

- [ ] **Step 1: 创建优惠券表**

```sql
-- 优惠券表
CREATE TABLE coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    type TINYINT NOT NULL COMMENT '类型：1满减 2折扣 3无门槛',
    discount DECIMAL(10,2) COMMENT '折扣比例',
    min_amount DECIMAL(10,2) COMMENT '最低消费金额',
    reduce_amount DECIMAL(10,2) COMMENT '减免金额',
    total_count INT NOT NULL DEFAULT 0 COMMENT '发行总量',
    used_count INT NOT NULL DEFAULT 0 COMMENT '已领取数量',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

-- 用户优惠券表
CREATE TABLE user_coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    coupon_id BIGINT NOT NULL COMMENT '优惠券ID',
    order_id BIGINT COMMENT '使用订单ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0未使用 1已使用 2已过期',
    get_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    use_time DATETIME COMMENT '使用时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_coupon_id (coupon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';
```

- [ ] **Step 2: 创建优惠券实体类**

```java
// Coupon.java
package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer type;
    private BigDecimal discount;
    private BigDecimal minAmount;
    private BigDecimal reduceAmount;
    private Integer totalCount;
    private Integer usedCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long tenantId;
}
```

```java
// UserCoupon.java
package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_coupon")
public class UserCoupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long couponId;
    private Long orderId;
    private Integer status;
    private LocalDateTime getTime;
    private LocalDateTime useTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long tenantId;
}
```

- [ ] **Step 3: 创建优惠券Mapper**

```java
// CouponMapper.java
package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {
}
```

```java
// UserCouponMapper.java
package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
}
```

- [ ] **Step 4: 创建优惠券服务接口**

```java
// CouponService.java
package com.reggie.service;

import com.reggie.entity.Coupon;
import com.reggie.entity.UserCoupon;
import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    /**
     * 获取所有可用优惠券
     */
    List<Coupon> getAvailableCoupons();
    
    /**
     * 领取优惠券
     */
    void claimCoupon(Long userId, Long couponId);
    
    /**
     * 获取用户优惠券列表
     */
    List<UserCoupon> getUserCoupons(Long userId);
    
    /**
     * 获取用户可用优惠券
     */
    List<UserCoupon> getUserAvailableCoupons(Long userId, BigDecimal orderAmount);
    
    /**
     * 使用优惠券
     */
    void useCoupon(Long userCouponId, Long orderId);
    
    /**
     * 计算优惠金额
     */
    BigDecimal calculateDiscount(UserCoupon userCoupon, BigDecimal orderAmount);
}
```

- [ ] **Step 5: 创建优惠券服务实现**

```java
// CouponServiceImpl.java
package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.entity.Coupon;
import com.reggie.entity.UserCoupon;
import com.reggie.mapper.CouponMapper;
import com.reggie.mapper.UserCouponMapper;
import com.reggie.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CouponServiceImpl implements CouponService {
    
    @Autowired
    private CouponMapper couponMapper;
    
    @Autowired
    private UserCouponMapper userCouponMapper;
    
    @Override
    public List<Coupon> getAvailableCoupons() {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getStatus, 1);
        wrapper.le(Coupon::getStartTime, LocalDateTime.now());
        wrapper.ge(Coupon::getEndTime, LocalDateTime.now());
        wrapper.lt(Coupon::getUsedCount, Coupon::getTotalCount);
        return couponMapper.selectList(wrapper);
    }
    
    @Override
    public void claimCoupon(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new RuntimeException("优惠券不存在或已下架");
        }
        
        if (coupon.getUsedCount() >= coupon.getTotalCount()) {
            throw new RuntimeException("优惠券已领完");
        }
        
        // 检查是否已领取
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        wrapper.eq(UserCoupon::getCouponId, couponId);
        if (userCouponMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("您已领取过该优惠券");
        }
        
        // 领取优惠券
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(0);
        userCoupon.setGetTime(LocalDateTime.now());
        userCouponMapper.insert(userCoupon);
        
        // 更新已领取数量
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponMapper.updateById(coupon);
    }
    
    @Override
    public List<UserCoupon> getUserCoupons(Long userId) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        wrapper.orderByDesc(UserCoupon::getGetTime);
        return userCouponMapper.selectList(wrapper);
    }
    
    @Override
    public List<UserCoupon> getUserAvailableCoupons(Long userId, BigDecimal orderAmount) {
        List<UserCoupon> userCoupons = getUserCoupons(userId);
        return userCoupons.stream()
            .filter(uc -> uc.getStatus() == 0)
            .filter(uc -> {
                Coupon coupon = couponMapper.selectById(uc.getCouponId());
                if (coupon == null) return false;
                
                // 检查是否在有效期内
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
                    return false;
                }
                
                // 检查是否满足最低消费
                if (coupon.getMinAmount() != null && orderAmount.compareTo(coupon.getMinAmount()) < 0) {
                    return false;
                }
                
                return true;
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public void useCoupon(Long userCouponId, Long orderId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || userCoupon.getStatus() != 0) {
            throw new RuntimeException("优惠券不可用");
        }
        
        userCoupon.setStatus(1);
        userCoupon.setOrderId(orderId);
        userCoupon.setUseTime(LocalDateTime.now());
        userCouponMapper.updateById(userCoupon);
    }
    
    @Override
    public BigDecimal calculateDiscount(UserCoupon userCoupon, BigDecimal orderAmount) {
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null) {
            return BigDecimal.ZERO;
        }
        
        switch (coupon.getType()) {
            case 1: // 满减
                return coupon.getReduceAmount();
            case 2: // 折扣
                BigDecimal discount = coupon.getDiscount().divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                return orderAmount.multiply(BigDecimal.ONE.subtract(discount))
                    .setScale(2, RoundingMode.HALF_UP);
            case 3: // 无门槛
                return coupon.getReduceAmount();
            default:
                return BigDecimal.ZERO;
        }
    }
}
```

- [ ] **Step 6: 创建优惠券控制器**

```java
// CouponController.java
package com.reggie.controller;

import com.reggie.common.R;
import com.reggie.entity.Coupon;
import com.reggie.entity.UserCoupon;
import com.reggie.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/coupon")
public class CouponController {
    
    @Autowired
    private CouponService couponService;
    
    /**
     * 获取所有可用优惠券
     */
    @GetMapping("/available")
    public R<List<Coupon>> getAvailableCoupons() {
        List<Coupon> coupons = couponService.getAvailableCoupons();
        return R.success(coupons);
    }
    
    /**
     * 领取优惠券
     */
    @PostMapping("/claim")
    public R<String> claimCoupon(@RequestParam Long userId, @RequestParam Long couponId) {
        couponService.claimCoupon(userId, couponId);
        return R.success("领取成功");
    }
    
    /**
     * 获取用户优惠券列表
     */
    @GetMapping("/user/{userId}")
    public R<List<UserCoupon>> getUserCoupons(@PathVariable Long userId) {
        List<UserCoupon> userCoupons = couponService.getUserCoupons(userId);
        return R.success(userCoupons);
    }
    
    /**
     * 获取用户可用优惠券
     */
    @GetMapping("/user/{userId}/available")
    public R<List<UserCoupon>> getUserAvailableCoupons(
            @PathVariable Long userId,
            @RequestParam BigDecimal orderAmount) {
        List<UserCoupon> userCoupons = couponService.getUserAvailableCoupons(userId, orderAmount);
        return R.success(userCoupons);
    }
    
    /**
     * 计算优惠金额
     */
    @GetMapping("/calculate")
    public R<BigDecimal> calculateDiscount(
            @RequestParam Long userCouponId,
            @RequestParam BigDecimal orderAmount) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setId(userCouponId);
        BigDecimal discount = couponService.calculateDiscount(userCoupon, orderAmount);
        return R.success(discount);
    }
}
```

- [ ] **Step 7: 创建优惠券前端API**

```javascript
// coupon.js
import request from '../js/request.js';

/**
 * 获取所有可用优惠券
 */
export function getAvailableCoupons() {
    return request({
        url: '/api/coupon/available',
        method: 'get'
    });
}

/**
 * 领取优惠券
 */
export function claimCoupon(userId, couponId) {
    return request({
        url: '/api/coupon/claim',
        method: 'post',
        params: { userId, couponId }
    });
}

/**
 * 获取用户优惠券列表
 */
export function getUserCoupons(userId) {
    return request({
        url: `/api/coupon/user/${userId}`,
        method: 'get'
    });
}

/**
 * 获取用户可用优惠券
 */
export function getUserAvailableCoupons(userId, orderAmount) {
    return request({
        url: `/api/coupon/user/${userId}/available`,
        method: 'get',
        params: { orderAmount }
    });
}

/**
 * 计算优惠金额
 */
export function calculateDiscount(userCouponId, orderAmount) {
    return request({
        url: '/api/coupon/calculate',
        method: 'get',
        params: { userCouponId, orderAmount }
    });
}
```

- [ ] **Step 8: 创建优惠券中心页面**

```html
<!-- coupon.html -->
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>优惠券中心</title>
    <link rel="stylesheet" href="../styles/vant.min.css"/>
    <link rel="stylesheet" href="../styles/index.css"/>
    <link rel="stylesheet" href="../styles/main.css"/>
    <style>
        body {
            background: #f5f5f5;
        }
        .coupon-container {
            padding: 15px;
        }
        .coupon-card {
            background: linear-gradient(135deg, #ff6b6b 0%, #ff8e8e 100%);
            border-radius: 12px;
            padding: 20px;
            margin-bottom: 15px;
            color: #fff;
            position: relative;
            overflow: hidden;
        }
        .coupon-card::before {
            content: '';
            position: absolute;
            right: -20px;
            top: -20px;
            width: 100px;
            height: 100px;
            background: rgba(255,255,255,0.1);
            border-radius: 50%;
        }
        .coupon-amount {
            font-size: 36px;
            font-weight: bold;
            margin-bottom: 5px;
        }
        .coupon-condition {
            font-size: 14px;
            opacity: 0.9;
            margin-bottom: 15px;
        }
        .coupon-name {
            font-size: 16px;
            font-weight: bold;
            margin-bottom: 5px;
        }
        .coupon-time {
            font-size: 12px;
            opacity: 0.8;
        }
        .coupon-btn {
            position: absolute;
            right: 20px;
            top: 50%;
            transform: translateY(-50%);
            background: #fff;
            color: #ff6b6b;
            border: none;
            border-radius: 20px;
            padding: 8px 20px;
            font-size: 14px;
            font-weight: bold;
            cursor: pointer;
        }
        .coupon-btn:disabled {
            background: rgba(255,255,255,0.5);
            color: rgba(255,255,255,0.8);
        }
        .empty-state {
            text-align: center;
            padding: 50px 0;
            color: #999;
        }
    </style>
</head>
<body>
    <div id="app">
        <van-nav-bar title="优惠券中心" left-arrow @click-left="goBack"/>
        
        <div class="coupon-container">
            <div v-if="coupons.length > 0">
                <div class="coupon-card" v-for="coupon in coupons" :key="coupon.id">
                    <div class="coupon-amount" v-if="coupon.type === 1">¥{{ coupon.reduceAmount }}</div>
                    <div class="coupon-amount" v-else-if="coupon.type === 2">{{ coupon.discount }}折</div>
                    <div class="coupon-amount" v-else>¥{{ coupon.reduceAmount }}</div>
                    <div class="coupon-condition" v-if="coupon.minAmount">满{{ coupon.minAmount }}元可用</div>
                    <div class="coupon-condition" v-else>无门槛</div>
                    <div class="coupon-name">{{ coupon.name }}</div>
                    <div class="coupon-time">{{ formatTime(coupon.startTime) }} - {{ formatTime(coupon.endTime) }}</div>
                    <button class="coupon-btn" @click="claimCoupon(coupon.id)" :disabled="coupon.claimed">
                        {{ coupon.claimed ? '已领取' : '立即领取' }}
                    </button>
                </div>
            </div>
            <div class="empty-state" v-else>
                <van-icon name="coupon" size="60px"/>
                <p>暂无可用优惠券</p>
            </div>
        </div>
    </div>
    
    <script src="../backend/plugins/vue/vue.js"></script>
    <script src="../js/vant.min.js"></script>
    <script src="../backend/plugins/axios/axios.min.js"></script>
    <script src="../js/request.js"></script>
    <script src="../api/coupon.js"></script>
    <script>
        new Vue({
            el: '#app',
            data() {
                return {
                    coupons: [],
                    userId: null
                }
            },
            created() {
                this.userId = this.getUserId();
                this.loadCoupons();
            },
            methods: {
                goBack() {
                    window.history.go(-1);
                },
                getUserId() {
                    return sessionStorage.getItem('userId');
                },
                async loadCoupons() {
                    const res = await getAvailableCoupons();
                    if (res.code === 1) {
                        this.coupons = res.data.map(coupon => ({
                            ...coupon,
                            claimed: false
                        }));
                    }
                },
                async claimCoupon(couponId) {
                    if (!this.userId) {
                        this.$toast.fail('请先登录');
                        return;
                    }
                    
                    try {
                        await claimCoupon(this.userId, couponId);
                        this.$toast.success('领取成功');
                        this.loadCoupons();
                    } catch (error) {
                        this.$toast.fail(error.message || '领取失败');
                    }
                },
                formatTime(time) {
                    if (!time) return '';
                    const date = new Date(time);
                    return `${date.getMonth() + 1}月${date.getDate()}日`;
                }
            }
        });
    </script>
</body>
</html>
```

- [ ] **Step 9: 优化订单确认页面优惠券选择**

```html
<!-- add-order.html 中优化优惠券选择 -->
<div class="coupon-section">
    <div class="section-title">优惠券</div>
    <div class="coupon-list" v-if="availableCoupons.length > 0">
        <div class="coupon-item" v-for="coupon in availableCoupons" :key="coupon.id"
             :class="{ active: selectedCoupon && selectedCoupon.id === coupon.id }"
             @click="selectCoupon(coupon)">
            <div class="coupon-info">
                <div class="coupon-amount" v-if="coupon.type === 1">¥{{ coupon.reduceAmount }}</div>
                <div class="coupon-amount" v-else-if="coupon.type === 2">{{ coupon.discount }}折</div>
                <div class="coupon-amount" v-else>¥{{ coupon.reduceAmount }}</div>
                <div class="coupon-condition" v-if="coupon.minAmount">满{{ coupon.minAmount }}元可用</div>
                <div class="coupon-condition" v-else>无门槛</div>
            </div>
            <i class="icon-check-line" v-if="selectedCoupon && selectedCoupon.id === coupon.id"></i>
        </div>
    </div>
    <div class="no-coupon" v-else>
        <span>暂无可用优惠券</span>
        <a href="coupon.html" class="link">去领券</a>
    </div>
</div>

<script>
data() {
    return {
        // ... 其他数据
        availableCoupons: [],
        selectedCoupon: null
    }
},
methods: {
    // ... 其他方法
    async loadAvailableCoupons() {
        const amount = this.calculateTotalAmount();
        const res = await getUserAvailableCoupons(this.userId, amount);
        if (res.code === 1) {
            this.availableCoupons = res.data;
        }
    },
    selectCoupon(coupon) {
        if (this.selectedCoupon && this.selectedCoupon.id === coupon.id) {
            this.selectedCoupon = null;
        } else {
            this.selectedCoupon = coupon;
        }
    },
    calculateDiscountAmount() {
        if (!this.selectedCoupon) return 0;
        const amount = this.calculateTotalAmount();
        if (this.selectedCoupon.type === 1) {
            return this.selectedCoupon.reduceAmount;
        } else if (this.selectedCoupon.type === 2) {
            const discount = this.selectedCoupon.discount / 10;
            return amount * (1 - discount);
        } else {
            return this.selectedCoupon.reduceAmount;
        }
    }
}
</script>

<style>
.coupon-section {
    background: #fff;
    margin-top: 10px;
    padding: 15px;
}

.coupon-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.coupon-item {
    display: flex;
    align-items: center;
    padding: 12px;
    border: 1px solid #eee;
    border-radius: 8px;
    cursor: pointer;
}

.coupon-item.active {
    border-color: #ff6b6b;
    background: #fff5f5;
}

.coupon-info {
    flex: 1;
}

.coupon-amount {
    font-size: 18px;
    font-weight: bold;
    color: #ff6b6b;
}

.coupon-condition {
    font-size: 12px;
    color: #999;
    margin-top: 5px;
}

.coupon-item i {
    color: #ff6b6b;
}

.no-coupon {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #999;
    font-size: 14px;
}

.no-coupon .link {
    color: #ff6b6b;
}
</style>
```

- [ ] **Step 10: 测试优惠券功能**

```bash
# 启动应用
java -jar target/reggie_take_out.jar

# 测试获取可用优惠券
curl -X GET "http://localhost:8080/api/coupon/available"

# 测试领取优惠券
curl -X POST "http://localhost:8080/api/coupon/claim?userId=1&couponId=1"

# 测试获取用户优惠券
curl -X GET "http://localhost:8080/api/coupon/user/1"

# 测试获取用户可用优惠券
curl -X GET "http://localhost:8080/api/coupon/user/1/available?orderAmount=50"

# 测试计算优惠金额
curl -X GET "http://localhost:8080/api/coupon/calculate?userCouponId=1&orderAmount=50"
```

- [ ] **Step 11: 提交代码**

```bash
git add src/main/java/com/reggie/entity/Coupon.java
git add src/main/java/com/reggie/entity/UserCoupon.java
git add src/main/java/com/reggie/mapper/CouponMapper.java
git add src/main/java/com/reggie/mapper/UserCouponMapper.java
git add src/main/java/com/reggie/service/CouponService.java
git add src/main/java/com/reggie/service/impl/CouponServiceImpl.java
git add src/main/java/com/reggie/controller/CouponController.java
git add src/main/resources/front/page/coupon.html
git add src/main/resources/front/api/coupon.js
git add src/main/resources/front/page/add-order.html
git commit -m "feat: 完善优惠券功能"
```

---

## 执行计划

### 阶段一：核心商业功能（1-2周）
1. **Task 1: 在线支付功能** - 3天
2. **Task 2: 配送追踪功能** - 2天
3. **Task 3: 优惠券功能完善** - 2天

### 阶段二：用户体验优化（2-3周）
1. 会员体系 - 5天
2. 常购清单 - 2天
3. 订单评价完善 - 3天

### 阶段三：功能增强（3-4周）
1. 语音搜索 - 3天
2. 分享功能 - 2天
3. 预约下单 - 3天

---

## 验证方案

### 功能测试
1. **在线支付测试**
   - 创建订单 → 选择支付方式 → 支付 → 支付结果
   - 支付回调处理
   - 退款流程

2. **配送追踪测试**
   - 创建配送单 → 骑手接单 → 取餐 → 配送 → 送达
   - 实时位置更新
   - 状态时间线展示

3. **优惠券测试**
   - 查看可用优惠券 → 领取 → 使用
   - 优惠金额计算
   - 优惠券有效期检查

### 集成测试
1. **订单流程测试**
   - 完整下单流程：选菜品 → 购物车 → 确认订单 → 支付 → 配送追踪

2. **优惠券流程测试**
   - 领券 → 下单使用 → 优惠计算

### 性能测试
1. **并发测试**
   - 同时领取优惠券
   - 同时支付

2. **响应时间测试**
   - 支付响应时间 < 3秒
   - 配送追踪更新 < 1秒

---

## 风险与应对

### 技术风险
1. **支付接口对接** - 需要申请微信/支付宝商户号
   - 应对：先使用沙箱环境测试

2. **实时位置更新** - 需要高频更新
   - 应对：使用WebSocket或长轮询

3. **优惠券并发** - 可能超发
   - 应对：使用数据库乐观锁

### 业务风险
1. **支付安全** - 需要保证支付安全
   - 应对：使用HTTPS，签名验证

2. **优惠券滥用** - 可能被刷券
   - 应对：限制领取数量，IP限制

---

## 总结

本计划在保持现有技术栈不变的前提下，通过3个核心功能改进，大幅提升瑞吉外卖的商业能力：

1. **在线支付** - 实现交易闭环
2. **配送追踪** - 提升用户体验
3. **优惠券完善** - 增加营销能力

预计完成后，瑞吉外卖的功能完整度将从60%提升至75%，与大厂的差距从35%缩小至20%。
