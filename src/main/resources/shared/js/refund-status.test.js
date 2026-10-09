// refund-status 行为断言（node src/main/resources/shared/js/refund-status.test.js）
// 锁定三端退款状态字典，防止状态文案漂移与大小写漏配回归（对应修复计划 P0-5 / 9.3）。
var assert = require('assert');
var fs = require('fs');
var path = require('path');
var vm = require('vm');

var src = fs.readFileSync(path.join(__dirname, 'refund-status.js'), 'utf8');
var sandbox = { window: {} };
vm.createContext(sandbox);
vm.runInContext(src, sandbox);
var D = sandbox.window.RefundStatusDict;

assert.ok(D, 'RefundStatusDict 应已挂载到 window');

// 1. 与后端 com.reggie.enums.RefundStatus 的 code 对齐
assert.strictEqual(D.refundText('pending'), '退款申请中');
assert.strictEqual(D.refundText('processing'), '退款处理中');
assert.strictEqual(D.refundText('SUCCESS'), '退款成功');
assert.strictEqual(D.refundText('fail'), '退款失败');
assert.strictEqual(D.refundText('rejected'), '审核拒绝');

// 2. 大小写不敏感（后端 SUCCESS 为大写、其余小写，字典按大写归一化查表）
assert.strictEqual(D.refundText('PENDING'), '退款申请中');
assert.strictEqual(D.refundText('Processing'), '退款处理中');
assert.strictEqual(D.refundText('success'), '退款成功');
assert.strictEqual(D.refundText('FAIL'), '退款失败');
assert.strictEqual(D.refundText('REJECTED'), '审核拒绝');

// 3. 空值 / 未知值的兜底
assert.strictEqual(D.refundText(null), '未知');
assert.strictEqual(D.refundText(''), '未知');
assert.strictEqual(D.refundText('WHATEVER'), 'WHATEVER');

// 4. 样式类与终态判定
assert.strictEqual(D.refundClass('pending'), 'st-pending');
assert.strictEqual(D.refundClass('SUCCESS'), 'st-success');
assert.strictEqual(D.isFinal('success'), true);
assert.strictEqual(D.isFinal('fail'), true);
assert.strictEqual(D.isFinal('rejected'), true);
assert.strictEqual(D.isFinal('pending'), false);
assert.strictEqual(D.isFinal('processing'), false);

console.log('refund-status.test.js: all assertions passed');
