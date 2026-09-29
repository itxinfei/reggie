// order-status 行为断言（node src/main/resources/shared/js/order-status.test.js）
// 锁定三端状态字典，防止状态文案漂移回归（对应修复计划 9.3）。
var assert = require('assert');
var fs = require('fs');
var path = require('path');
var vm = require('vm');

var src = fs.readFileSync(path.join(__dirname, 'order-status.js'), 'utf8');
var sandbox = { window: {} };
vm.createContext(sandbox);
vm.runInContext(src, sandbox);
var D = sandbox.window.OrderStatusDict;

assert.ok(D, 'OrderStatusDict 应已挂载到 window');

// 1. 主单状态文案与后端 enums.OrderStatus 严格对齐
assert.strictEqual(D.orderText(1), '待付款');
assert.strictEqual(D.orderText(2), '待接单');
assert.strictEqual(D.orderText(3), '配送中');
assert.strictEqual(D.orderText(4), '已完成');
assert.strictEqual(D.orderText(5), '已取消');
assert.strictEqual(D.orderText(6), '已退款');
assert.strictEqual(D.orderText(7), '已分账');
// 未知值回退
assert.strictEqual(D.orderText(99), '未知');

// 2. 主单状态样式类存在
assert.strictEqual(D.orderClass(2), 'status-accepted');
assert.strictEqual(D.orderClass(4), 'status-completed');

// 3. 配送状态文案与后端 DeliveryOrderStatus 对齐
assert.strictEqual(D.deliveryText('PENDING'), '待接单');
assert.strictEqual(D.deliveryText('ACCEPTED'), '已接单');
assert.strictEqual(D.deliveryText('PICKING'), '取餐中');
assert.strictEqual(D.deliveryText('DELIVERING'), '配送中');
assert.strictEqual(D.deliveryText('DELIVERED'), '已送达');
assert.strictEqual(D.deliveryText('CANCELLED'), '已取消');
assert.strictEqual(D.deliveryText('UNKNOWN'), '未知状态');

// 4. 配送状态描述与图标非空，终态取消有图标
assert.ok(D.deliveryDesc('DELIVERING').length > 0);
assert.strictEqual(D.deliveryIcon('DELIVERED'), 'ri-check-line');
assert.strictEqual(D.deliveryIcon('CANCELLED'), 'ri-close-line');

// 5. 配送样式类：PICKING 与 DELIVERING 区分但语义色正确
assert.strictEqual(D.deliveryClass('PICKING'), 'status-accepted');
assert.strictEqual(D.deliveryClass('DELIVERING'), 'status-shipped');
assert.strictEqual(D.deliveryClass('DELIVERED'), 'status-completed');

console.log('order-status.test.js: all passed');
