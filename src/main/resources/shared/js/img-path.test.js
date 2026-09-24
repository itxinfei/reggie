// img-path 行为断言（node src/main/resources/shared/js/img-path.test.js）
var assert = require('assert');
var fs = require('fs');
var path = require('path');
var vm = require('vm');

var src = fs.readFileSync(path.join(__dirname, 'img-path.js'), 'utf8');
var sandbox = {};
vm.createContext(sandbox);
vm.runInContext(src, sandbox);
var imgPath = sandbox.imgPath;

// 1. 空值 → 占位图（node 无 location，回退 front 默认）
assert.strictEqual(imgPath(''), '/front/images/noImg.png');
assert.strictEqual(imgPath(null), '/front/images/noImg.png');
assert.strictEqual(imgPath(undefined), '/front/images/noImg.png');

// 2. 外链原样返回
assert.strictEqual(imgPath('https://cdn.example.com/a.jpg'), 'https://cdn.example.com/a.jpg');
assert.strictEqual(imgPath('http://cdn.example.com/a.jpg'), 'http://cdn.example.com/a.jpg');

// 3. 已带 /common/download 前缀不重复拼接
assert.strictEqual(
  imgPath('/common/download?name=images/dishes/x.jpg'),
  '/common/download?name=images/dishes/x.jpg'
);

// 4. 站内绝对路径原样返回
assert.strictEqual(imgPath('/front/images/logo.png'), '/front/images/logo.png');

// 5. public 前缀 → 静态直出
assert.strictEqual(
  imgPath('public/admin/dishes/202609/abc.jpg'),
  '/uploads/public/admin/dishes/202609/abc.jpg'
);

// 6. private 与旧相对路径 → encode 后走 download
assert.strictEqual(
  imgPath('private/admin/purchase/202609/abc.jpg'),
  '/common/download?name=' + encodeURIComponent('private/admin/purchase/202609/abc.jpg')
);
assert.strictEqual(
  imgPath('images/dishes/中 文.jpg'),
  '/common/download?name=' + encodeURIComponent('images/dishes/中 文.jpg')
);

console.log('img-path.test.js: all passed');
