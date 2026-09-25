// gen-placeholders.js — 为 DB 中引用但磁盘缺失的图片生成合法占位图
// 使用 PNG 手写编码器生成纯色+尺寸信息的占位图，保证 file 命令识别为合法 PNG/JPEG
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const UP = path.normalize('D:/MyCode/reggie/uploads');

// ---------- 极简 PNG 编码（单色） ----------
function crc32(buf) {
  let table = crc32.table;
  if (!table) {
    table = crc32.table = [];
    for (let n = 0; n < 256; n++) {
      let c = n;
      for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
      table[n] = c >>> 0;
    }
  }
  let crc = 0xffffffff;
  for (let i = 0; i < buf.length; i++) crc = table[(crc ^ buf[i]) & 0xff] ^ (crc >>> 8);
  return (crc ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length, 0);
  const td = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(td), 0);
  return Buffer.concat([len, td, crc]);
}
function makePng(w, h, rgb) {
  const sig = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4);
  ihdr[8] = 8; ihdr[9] = 2; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
  const row = Buffer.alloc(1 + w * 3);
  row[0] = 0;
  for (let x = 0; x < w; x++) { row[1 + x * 3] = rgb[0]; row[2 + x * 3] = rgb[1]; row[3 + x * 3] = rgb[2]; }
  const raw = Buffer.concat(Array.from({ length: h }, () => row));
  const idat = zlib.deflateSync(raw);
  return Buffer.concat([sig, chunk('IHDR', ihdr), chunk('IDAT', idat), chunk('IEND', Buffer.alloc(0))]);
}

// ---------- 颜色方案 ----------
const PALETTE = [
  [255, 194, 0], [240, 98, 146], [76, 175, 80], [33, 150, 243], [156, 39, 176],
  [255, 87, 34], [0, 188, 212], [255, 152, 0], [63, 81, 181], [121, 85, 72],
  [103, 58, 183], [2, 119, 189], [194, 24, 91], [124, 179, 66], [239, 108, 0]
];

console.log('===== 生成供应商营业执照占位图 =====');
const SUP = path.join(UP, 'images', 'suppliers');
fs.mkdirSync(SUP, { recursive: true });
const supNames = ['京华食材配送中心', '永辉生鲜', '鑫隆肉业', '海之鲜水产', '金龙鱼粮油', '味事达调味', '老北京豆制品', '农夫山泉', '绿萌食品', '冷链速配', '丰乐农场', '康美食品', '优品包材', '顺发冻品', '华鲜果蔬'];
for (let i = 0; i < 15; i++) {
  const rgb = PALETTE[i % PALETTE.length];
  const p = path.join(SUP, 'license-' + (i + 1) + '.png');
  fs.writeFileSync(p, makePng(480, 360, rgb));
  console.log('  license-' + (i + 1) + '.png  ' + supNames[i] + '  ' + fs.statSync(p).size + 'B');
}

(async () => {
  const c = await mysql.createConnection({ host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'reggie' });
  const [sup] = await c.query('SELECT id,name,LICENSE_IMAGES FROM supplier ORDER BY id');
  let n = 0;
  for (const s of sup) {
    const cur = s.LICENSE_IMAGES ? String(s.LICENSE_IMAGES) : '';
    if (!/\.png$/i.test(cur)) {
      await c.query('UPDATE supplier SET LICENSE_IMAGES=? WHERE id=?', ['images/suppliers/license-' + s.id + '.png', s.id]);
      n++;
    }
  }
  console.log('\nsupplier.LICENSE_IMAGES 修正 ' + n + ' 行');

  // 门店执照
  const [t] = await c.query('SELECT id,name FROM tenant ORDER BY id');
  let m = 0;
  for (let i = 0; i < t.length; i++) {
    const p = path.join(UP, 'images', 'logos', 'store-license-' + t[i].id + '.png');
    fs.writeFileSync(p, makePng(480, 360, PALETTE[i % PALETTE.length]));
    await c.query('UPDATE tenant SET license_image=? WHERE id=?', ['images/logos/store-license-' + t[i].id + '.png', t[i].id]);
    m++;
  }
  console.log('tenant.license_image 生成 ' + m + ' 张');
  await c.end();
})();
