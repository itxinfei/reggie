// 幂等补全实体审计字段（createUser/updateUser/updateTime），使 Java 实体与 DB 列完全对齐。
// 运行：node scripts/add-audit-fields.js   （改完后用 mvn compile + 全量 test 验证）
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');

// 待补清单：文件相对路径 -> 缺失列（来自 db-java-gap 实时分析）
const TASKS = [
  ['src/main/java/com/reggie/module/member/model/CouponTemplate.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/member/model/CouponUser.java', ['update_time', 'create_user', 'update_user']],
  ['src/main/java/com/reggie/module/dining/model/TableArea.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/dining/model/QueueRecord.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/dining/model/Reservation.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/dining/model/DiningTable.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/marketing/model/MarketingMessage.java', ['create_user', 'update_time', 'update_user']],
  ['src/main/java/com/reggie/module/inventory/model/Material.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/inventory/model/MaterialCategory.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/member/model/Member.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/member/model/MemberLevel.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/member/model/MemberTag.java', ['update_time', 'update_user']],
  ['src/main/java/com/reggie/module/payment/model/PaymentChannelConfig.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/payment/model/PaymentOrder.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/member/model/PointsRecord.java', ['update_time', 'create_user', 'update_user']],
  ['src/main/java/com/reggie/module/inventory/model/PurchaseOrder.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/inventory/model/PurchaseOrderDetail.java', ['update_time', 'create_user', 'update_user']],
  ['src/main/java/com/reggie/module/member/model/RechargeRecord.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/recommend/model/RecommendationCache.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/recommend/model/RecommendationFeedback.java', ['create_user', 'update_time', 'update_user']],
  ['src/main/java/com/reggie/module/payment/model/RefundRecord.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/inventory/model/StockCheck.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/inventory/model/StockCheckDetail.java', ['update_time', 'create_user', 'update_user']],
  ['src/main/java/com/reggie/module/inventory/model/StockRecord.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/store/model/StoreSyncLog.java', ['update_time', 'create_user', 'update_user']],
  ['src/main/java/com/reggie/module/inventory/model/Supplier.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/inventory/model/SupplierSettlement.java', ['create_user', 'update_user']],
  ['src/main/java/com/reggie/module/user/model/User.java', ['update_time']],
  ['src/main/java/com/reggie/module/notification/model/UserDevice.java', ['create_user', 'update_user']],
];

const FIELD_DEF = {
  create_user: {
    field: 'createUser', type: 'Long', fill: 'FieldFill.INSERT', comment: '创建人',
  },
  update_user: {
    field: 'updateUser', type: 'Long', fill: 'FieldFill.INSERT_UPDATE', comment: '修改人',
  },
  update_time: {
    field: 'updateTime', type: 'LocalDateTime', fill: 'FieldFill.INSERT_UPDATE', comment: '更新时间',
  },
};
const ORDER = ['create_user', 'update_user', 'update_time'];

function hasField(text, fieldName) {
  return new RegExp('\\bprivate\\s+[\\w.<>\\[\\]]+\\s+' + fieldName + '\\s*[;=]').test(text);
}

function addImports(text, needTime) {
  const lines = text.split('\n');
  let lastImport = -1;
  let hasAnno = false, hasFill = false, hasTime = false;
  lines.forEach((l, i) => {
    if (/^import\s+/.test(l)) lastImport = i;
    if (/^import\s+com\.baomidou\.mybatisplus\.annotation\.(TableField|\*)/.test(l)) hasAnno = true;
    if (/^import\s+com\.baomidou\.mybatisplus\.annotation\.(FieldFill|\*)/.test(l)) hasFill = true;
    if (/^import\s+java\.time\.LocalDateTime;/.test(l)) hasTime = true;
  });
  const add = [];
  if (!hasAnno) add.push('import com.baomidou.mybatisplus.annotation.TableField;');
  if (!hasFill) add.push('import com.baomidou.mybatisplus.annotation.FieldFill;');
  if (needTime && !hasTime) add.push('import java.time.LocalDateTime;');
  if (!add.length || lastImport < 0) return text;
  lines.splice(lastImport + 1, 0, ...add);
  return lines.join('\n');
}

function insertFields(text, cols) {
  const blocks = [];
  for (const c of ORDER) {
    if (!cols.includes(c)) continue;
    const d = FIELD_DEF[c];
    blocks.push(
      '    /** ' + d.comment + ' */\n' +
      '    @TableField(fill = ' + d.fill + ')\n' +
      '    private ' + d.type + ' ' + d.field + ';'
    );
  }
  // 找最后一个“非 static 实例字段”声明作为锚点
  const re = /^    (?:private|protected|public)\s+(?!static\b)[^\n;]*;/gm;
  let match; let last = null;
  while ((match = re.exec(text)) !== null) last = match;
  if (!last) return text;
  const insertAt = last.index + last[0].length;
  return text.slice(0, insertAt) + '\n\n' + blocks.join('\n\n') + text.slice(insertAt);
}

let changedFiles = 0; let addedFields = 0; const skipped = [];
for (const [rel, cols] of TASKS) {
  const file = path.join(ROOT, rel);
  let text = fs.readFileSync(file, 'utf8');
  if (/\bclass\s+\w+\s+extends\b/.test(text)) { skipped.push(rel + '（含继承，手动确认）'); continue; }

  const todo = cols.filter((c) => FIELD_DEF[c] && !hasField(text, FIELD_DEF[c].field));
  if (!todo.length) continue;

  const needTime = todo.includes('update_time');
  text = addImports(text, needTime);
  text = insertFields(text, todo);
  fs.writeFileSync(file, text, 'utf8');
  changedFiles++; addedFields += todo.length;
}

console.log('修改实体文件数:', changedFiles, ' 新增字段数:', addedFields);
if (skipped.length) console.log('需手动确认:\n' + skipped.map((s) => '  - ' + s).join('\n'));
