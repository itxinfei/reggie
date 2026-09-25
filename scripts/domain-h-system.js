// domain-h-system.js — 系统域(租户/门店/菜单权限/系统配置/AI/通知/发票/客服/平台/打印/日志)
// 用法: node scripts/domain-h-system.js
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');
const OUT = path.join(__dirname, '..', 'src/main/resources/db/seed/domain-h-system.sql');

let _s = 20260925;
const rnd = () => { _s = (_s * 1103515245 + 12345) & 0x7fffffff; return _s / 0x7fffffff; };
const ri = (a, b) => a + Math.floor(rnd() * (b - a + 1));
const pick = a => a[Math.floor(rnd() * a.length)];
const rf = (a, b, d = 2) => +((a + rnd() * (b - a)).toFixed(d));
const pad2 = n => String(n).padStart(2, '0');
const dt = d => d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate()) + ' ' + pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ':' + pad2(d.getSeconds());

(async () => {
  const c = await mysql.createConnection({ host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'reggie', charset: 'utf8mb4' });
  const q = async s => { const [r] = await c.query(s); return r; };
  const NOW = new Date(2026, 8, 25, 18, 0, 0);
  const ago = (d, h) => { const x = new Date(NOW); x.setDate(x.getDate() - d); x.setHours(h || ri(9, 22), ri(0, 59), 0, 0); return x; };
  const later = (d, h) => { const x = new Date(NOW); x.setDate(x.getDate() + d); x.setHours(h || 12, 0, 0, 0); return x; };

  const users = (await q('SELECT id,name,tenant_id FROM user ORDER BY id')).map(r => ({ id: +r.id, name: r.name, tid: +r.tenant_id }));
  const orders = (await q('SELECT id,order_time,amount,user_id FROM orders WHERE is_deleted=0 ORDER BY id')).map(r => ({ id: +r.id, t: r.order_time, amt: +r.amount, uid: +r.user_id }));
  const dishes = (await q('SELECT id,name,price FROM dish WHERE is_deleted=0 ORDER BY id')).map(r => ({ id: +r.id, name: r.name, price: +r.price }));
  const staff = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];
  const TID = 1;

  // ================= 1. tenant 10 行 =================
  const tenants = [
    [1, '瑞吉主门店', '王建国', '北京市朝阳区望京街道广顺北大街33号院', 100000, 1],
    [1000, '瑞吉东城店', '李伟', '北京市东城区建国门内大街5号', 80000, 1],
    [1001, '瑞吉海淀店', '张伟', '北京市海淀区中关村大街27号', 75000, 1],
    [1002, '瑞吉朝阳店', '刘洋', '北京市朝阳区建国路93号院', 70000, 1],
    [1003, '瑞吉西城店', '陈芳', '北京市西城区金融大街28号', 65000, 1],
    [1004, '瑞吉丰台店', '赵磊', '北京市丰台区南三环西路5号', 60000, 1],
    [1005, '瑞吉通州店', '孙丽', '北京市通州区新华大街16号', 55000, 1],
    [1006, '瑞吉石景山店', '周敏', '北京市石景山区鲁谷路12号', 50000, 1],
    [1007, '瑞吉昌平店', '吴强', '北京市昌平区回龙观东大街18号', 45000, 1],
    [1008, '瑞吉大兴店', '郑涛', '北京市大兴区黄村西大街33号', 40000, 0]
  ].map(t => ({
    id: t[0], name: t[1], phone: '010-' + (60000000 + t[0] * 7),
    address: t[3], contact: t[2], logo: null, license_image: null,
    package_name: t[0] === 1 ? '旗舰版' : (t[4] > 60000 ? '企业版' : '标准版'),
    expire_time: later(ri(100, 400)), password_type: 'BCRYPT', status: t[5],
    create_time: ago(ri(300, 600)), update_time: NOW, create_user: 1, update_user: 1
  }));

  // ================= 2. store =================
  const stores = tenants.map((t, i) => ({
    id: i + 1, name: t.name, address: t.address, phone: t.phone,
    business_hours: '10:00-22:00', logo: null,
    status: t.status, create_time: ago(ri(300, 600)), update_time: NOW,
    is_deleted: 0, tenant_id: t.id
  }));

  // ================= 3. store_info =================
  const storeInfo = [
    [1, 1, 1, 1], [1000, 1, 2, 1], [1001, 1, 2, 1], [1002, 1, 2, 1], [1003, 1, 2, 1],
    [1004, 1, 2, 1], [1005, 1, 2, 1], [1006, 1, 2, 1], [1007, 1, 2, 1], [1008, 1, 2, 0]
  ].map((s, i) => ({
    id: i + 1, tenant_id: s[1], store_code: 'STORE' + pad2(i + 1).padStart(3, '0'),
    store_type: s[2], parent_tenant_id: 1,
    business_hours: '10:00-22:00', delivery_radius: ri(3, 6),
    min_delivery_amount: pick([15, 20, 25]), delivery_fee: pick([3, 4, 5]),
    is_delivery_enabled: 1, is_dine_in_enabled: 1,
    contact_person: pick(['王建国', '李伟', '张伟', '刘洋', '陈芳', '赵磊']),
    contact_phone: '139' + String(10000000 + i * 53).slice(0, 8),
    longitude: rf(116.3, 116.5), latitude: rf(39.8, 40.1),
    pause_order: s[3] === 0 ? 1 : 0,
    create_time: ago(ri(200, 500)), update_time: NOW, create_user: 1, update_user: 1, is_deleted: 0
  }));

  // ================= 4. role 10 行 =================
  const roles = [
    ['超级管理员', 'SUPER_ADMIN', 1], ['店长', 'STORE_MANAGER', 2], ['副店长', 'DEPUTY_MANAGER', 3],
    ['大堂经理', 'HALL_MANAGER', 4], ['收银员', 'CASHIER', 5], ['厨师长', 'HEAD_CHEF', 6],
    ['厨师', 'CHEF', 7], ['服务员', 'WAITER', 8], ['采购员', 'PURCHASER', 9], ['仓库管理员', 'WAREHOUSE_MANAGER', 10]
  ].map((r, i) => ({
    id: i + 1, tenant_id: TID, role_name: r[0], role_key: r[1],
    description: r[0] + '角色，负责对应业务模块的管理',
    sort: r[2], status: 1, create_time: ago(ri(500, 800)), update_time: NOW,
    create_user: 1, update_user: 1, is_deleted: 0
  }));

  // ================= 5. menu 30 行（目录 + 菜单 + 按钮）=================
  const menus = [
    [null, '菜品管理', '/dish', 'Dish', 'dish:list', '🍲', 1, 1, 1],
    [null, '套餐管理', '/setmeal', 'Setmeal', 'setmeal:list', '📋', 1, 1, 2],
    [null, '订单管理', '/order', 'Order', 'order:list', '📦', 1, 1, 3],
    [null, '用户管理', '/user', 'User', 'user:list', '👤', 1, 1, 4],
    [null, '营销管理', '/marketing', 'Marketing', 'marketing:list', '🎁', 1, 1, 5],
    [null, '库存管理', '/inventory', 'Inventory', 'inventory:list', '📦', 1, 1, 6],
    [null, '财务结算', '/finance', 'Finance', 'finance:list', '💰', 1, 1, 7],
    [null, '系统设置', '/system', 'System', 'system:list', '⚙️', 1, 1, 8],
    [null, '数据报表', '/report', 'Report', 'report:list', '📊', 1, 1, 9],
    [null, '打印管理', '/printer', 'Printer', 'printer:list', '🖨️', 1, 1, 10],
    [null, '客服管理', '/cs', 'CustomerService', 'cs:list', '💬', 1, 1, 11],
    [null, 'AI助手', '/ai', 'AiAssistant', 'ai:list', '🤖', 1, 1, 12]
  ].map((m, i) => ({
    id: i + 1, parent_id: m[0], name: m[1], path: m[2], component: m[3],
    perms: m[4], icon: m[5], type: 2, sort: m[8], status: 1,
    create_time: ago(ri(500, 800)), update_time: NOW, is_deleted: 0
  }));
  // 按钮：每个菜单 2-3 个操作按钮
  const menuBtns = [];
  let bid = menus.length;
  for (let i = 0; i < 10; i++) {
    const btns = ['新增', '编辑', '删除', '导出'];
    const n = ri(2, 3);
    for (let j = 0; j < n; j++) {
      bid++;
      menuBtns.push({
        id: bid, parent_id: i + 1, name: menus[i].name + '-' + btns[j],
        path: '', component: '', perms: menus[i].perms + ':' + ['add', 'edit', 'delete', 'export'][j],
        icon: '', type: 3, sort: j + 1, status: 1,
        create_time: ago(ri(500, 800)), update_time: NOW, is_deleted: 0
      });
    }
  }
  const allMenus = [...menus, ...menuBtns];

  // ================= 6. permission 10 行 =================
  const perms = allMenus.slice(0, 10).map((m, i) => ({
    id: i + 1, permission_name: m.name, permission_key: m.perms,
    permission_type: 1, parent_id: 0, route_path: m.path, icon: m.icon,
    sort: i + 1, status: 1, create_time: ago(ri(500, 800)), update_time: NOW
  }));

  // ================= 7. role_permission =================
  const rolePerms = [];
  roles.forEach(r => {
    const n = ri(3, perms.length);
    const picked = [...perms].sort(() => rnd() - 0.5).slice(0, n);
    picked.forEach(p => rolePerms.push({ id: rolePerms.length + 1, role_id: r.id, permission_id: p.id, create_time: ago(ri(400, 800)) }));
  });

  // ================= 8. employee 15 行 =================
  const empNames = [
    ['admin', '王建国', '13901010001', '男', '店长', 'MG001'],
    ['zhangwei', '张伟', '13901010002', '男', '厨师长', 'CF001'],
    ['liwei', '李伟', '13901010003', '男', '厨师', 'CF002'],
    ['chenfang', '陈芳', '13901010004', '女', '服务员', 'SR001'],
    ['zhaolei', '赵磊', '13901010005', '男', '收银员', 'CS001'],
    ['sunli', '孙丽', '13901010006', '女', '收银员', 'CS002'],
    ['zhoumin', '周敏', '13901010007', '女', '大堂经理', 'SR002'],
    ['wuqiang', '吴强', '13901010008', '男', '采购员', 'PU001'],
    ['zhengtao', '郑涛', '13901010009', '男', '仓库管理员', 'WH001'],
    ['liujie', '刘杰', '13901010010', '男', '厨师', 'CF003'],
    ['wangmei', '王梅', '13901010011', '女', '服务员', 'SR003'],
    ['yanggang', '杨刚', '13901010012', '男', '服务员', 'SR004'],
    ['hejing', '何静', '13901010013', '女', '收银员', 'CS003'],
    ['gaojun', '高军', '13901010014', '男', '厨师', 'CF004'],
    ['luoping', '吕萍', '13901010015', '女', '服务员', 'SR005']
  ];
  const pwdHash = '$2a$10$' + 'N9qo8uLOickgx2ZMRZoMye'.padEnd(53, 'a');
  const employees = empNames.map((e, i) => ({
    id: i + 1, username: e[0], name: e[1], phone: e[2],
    sex: e[3] === '男' ? '1' : '2',
    id_number: '11010' + String(19800101 + i * 37).slice(0, 8) + ri(100, 999) + ri(1, 9),
    avatar: null, job_number: e[5], position: e[4], status: i < 2 ? 0 : 1,
    create_time: ago(ri(300, 800)), update_time: NOW, create_user: 1, update_user: 1,
    password: i === 0 ? '0192023a7bbd73250516f069df18b500' : pwdHash,
    password_type: 'BCRYPT', tenant_id: TID, role: i + 1
  }));

  // ================= 9. employee_role =================
  const empRoles = employees.map((e, i) => ({
    id: i + 1, employee_id: e.id, role_id: i + 1, tenant_id: TID,
    create_time: ago(ri(300, 800))
  }));

  // ================= 10. system_config 30 行 =================
  const configs = [
    ['store.name', '瑞吉主门店', 2], ['store.address', '北京市朝阳区望京街道广顺北大街33号院', 2],
    ['store.phone', '010-60010000', 2], ['store.business_hours', '10:00-22:00', 2],
    ['store.pause_order', '0', 1], ['store.delivery_radius', '5', 1],
    ['order.min_delivery_amount', '20.00', 1], ['order.delivery_fee', '4.00', 1],
    ['order.packaging_fee', '2.00', 1], ['order.auto_accept', '1', 1],
    ['order.refund_deadline_hours', '24', 1], ['order.cancel_deadline_minutes', '15', 1],
    ['pay.wechat_enabled', '1', 1], ['pay.alipay_enabled', '1', 1],
    ['pay.unionpay_enabled', '0', 1], ['pay.cash_enabled', '1', 1],
    ['notify.sms_enabled', '1', 1], ['notify.sms_provider', 'aliyun', 2],
    ['notify.sms_prefix', '【瑞吉外卖】', 2], ['notify.push_enabled', '1', 1],
    ['invoice.enabled', '1', 1], ['invoice.default_title', '瑞吉餐饮（北京）有限公司', 2],
    ['invoice.tax_number', '91110105MA01ABC123', 2], ['invoice.default_type', '1', 1],
    ['ai.provider', 'openai', 2], ['ai.model', 'gpt-4o', 2],
    ['ai.temperature', '0.7', 1], ['ai.max_tokens', '2048', 1],
    ['system.version', '2.0.0', 2], ['system.theme', 'light', 2]
  ].map((c, i) => ({
    id: i + 1, tenant_id: TID, config_key: c[0], config_value: String(c[1]),
    config_type: c[2], description: c[0].replace(/\./g, ' / ') + ' 配置项',
    create_time: ago(ri(400, 800)), update_time: NOW, create_user: 1, update_user: 1
  }));

  // ================= 11. ai_prompt_template 12 =================
  const aiPrompts = [
    ['recommend', '智能点餐推荐', 'assistant', '你是瑞吉外卖的AI点餐助手，请根据用户口味偏好和场景推荐合适菜品。', 1],
    ['greeting', '问候语', 'assistant', '你是友好的瑞吉外卖点餐助手，请用亲切的语气问候用户。', 1],
    ['complaint', '投诉处理', 'assistant', '你是专业的客户服务代表，请认真倾听用户投诉，给出恰当的解决方案。', 0],
    ['order_confirm', '订单确认', 'assistant', '你是订单确认助手，请准确复述用户订单内容并提示用户确认。', 1],
    ['ingredient_q', '食材咨询', 'assistant', '你是菜品知识助手，请基于菜单数据回答关于食材、口味、做法的问题。', 1],
    ['nutrition', '营养咨询', 'assistant', '你是营养顾问，请客观说明菜品营养情况，不要做医疗建议。', 1],
    ['promo_explain', '优惠说明', 'assistant', '你是营销助手，请准确说明优惠券和促销活动的适用条件。', 1],
    ['delivery_q', '配送咨询', 'assistant', '你是配送助手，请回答关于配送范围、时效、费用的问题。', 1],
    ['allergic_advice', '过敏建议', 'assistant', '你是过敏咨询助手，请谨慎回答用户过敏相关问题，必要时建议咨询医生。', 1],
    ['feedback_sum', '反馈总结', 'assistant', '你是用户反馈分析助手，请总结评价内容，提取改进建议。', 0],
    ['member_service', '会员服务', 'assistant', '你是会员助手，请回答积分、会员等级、优惠活动相关问题。', 1],
    ['default', '默认助手', 'assistant', '你是瑞吉外卖AI助手，请基于菜单和订单数据提供准确、有用的服务。', 1]
  ].map((p, i) => ({
    id: i + 1, code: p[0], scene: p[1], type: p[2], title: p[1],
    content: p[3], quick_questions: null, builtin: p[4],
    enabled: p[4], sort: i + 1, version: 1,
    create_time: ago(ri(200, 400)), update_time: NOW,
    create_user: 1, update_user: 1, is_deleted: 0
  }));

  // ================= 12. ai_provider_config 5 =================
  const aiProviders = [
    ['openai', 'OpenAI', 'https://api.openai.com/v1', 'gpt-4o', 'OPENAI_COMPATIBLE', 'message.choices[0].message.content', 1, 1],
    ['anthropic', 'Anthropic Claude', 'https://api.anthropic.com/v1', 'claude-3-5-sonnet', 'ANTHROPIC', 'content[0].text', 1, 0],
    ['deepseek', 'DeepSeek', 'https://api.deepseek.com/v1', 'deepseek-chat', 'OPENAI_COMPATIBLE', 'message.content', 1, 1],
    ['dashscope', '通义千问', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'qwen-plus', 'OPENAI_COMPATIBLE', 'message.content', 1, 0],
    ['gemini', 'Google Gemini', 'https://generativelanguage.googleapis.com/v1beta', 'gemini-1.5-pro', 'GEMINI', 'candidates[0].content.parts[0].text', 0, 0]
  ].map((p, i) => ({
    id: i + 1, provider_code: p[0], provider_name: p[1], base_url: p[2],
    model_name: p[3], api_key: 'sk-' + (10000000 + i * 137).toString(16),
    timeout: 30000, max_tokens: 2048, temperature: 0.7,
    api_format: p[4], extra_headers: null, request_template: null,
    response_path: p[5], icon_url: null,
    enabled: p[6], is_active: p[7],
    last_test_time: ago(ri(0, 10)), last_test_result: p[6] ? 1 : null,
    sort: i + 1, remark: null, create_time: ago(ri(200, 400)),
    update_time: NOW, create_user: 1, update_user: 1, is_deleted: 0,
    capabilities: 'chat', embedding_dimensions: 1536
  }));

  // ================= 13. notification_template 10 =================
  const notifyTemplates = [
    ['ORDER_CREATED', '订单创建成功', 'order', '您的订单已创建成功，请确认收货地址', '订单'],
    ['ORDER_PAID', '支付成功', 'order', '感谢您的支付，正在为您准备订单', '订单'],
    ['ORDER_DELIVERED', '订单已送达', 'order', '您的订单已送达，请确认收货', '订单'],
    ['REFUND_SUCCESS', '退款成功', 'order', '您的退款已处理成功', '订单'],
    ['COUPON_GRANT', '优惠券到账', 'coupon', '您已获得优惠券', '优惠'],
    ['COUPON_EXPIRE', '优惠券即将过期', 'coupon', '您的优惠券即将过期，请及时使用', '优惠'],
    ['POINTS_EXPIRE', '积分即将过期', 'points', '您的积分即将过期，请尽快使用', '积分'],
    ['NEW_DISH', '新品上架', 'dish', '瑞吉外卖新品上线啦', '菜品'],
    ['PROMO_START', '促销活动开始', 'promo', '瑞吉外卖促销活动火热进行中', '促销'],
    ['MEMBER_UPGRADE', '会员等级提升', 'member', '恭喜您，会员等级已提升', '会员']
  ].map((t, i) => ({
    id: i + 1, tenant_id: TID, template_name: t[1], template_code: t[0],
    channel: pick([1, 2, 3, 4]), biz_type: t[2],
    title: t[1], content: t[3], param_list: JSON.stringify(['orderNo', 'amount']),
    sign_name: '瑞吉外卖', status: 1, remark: null,
    create_time: ago(ri(200, 400)), update_time: NOW, create_user: 1, update_user: 1, is_deleted: 0
  }));

  // ================= 14. notification_record 60 =================
  const notifyRecords = [];
  const bizTypes = ['order', 'coupon', 'points', 'promo'];
  const channels = [1, 2, 3, 4];
  for (let i = 0; i < 60; i++) {
    const tpl = pick(notifyTemplates);
    const ch = pick(channels);
    const st = rnd() < 0.85 ? 2 : 1;
    notifyRecords.push({
      id: i + 1, tenant_id: TID, template_id: tpl.id, biz_type: tpl.biz_type,
      channel: ch, target_type: pick([1, 2]),
      target_value: String(pick(users).id), target_count: ri(1, 50),
      content: tpl.content, send_time: ago(ri(0, 25), ri(9, 22)),
      status: st, success_count: st === 2 ? ri(1, 50) : 0,
      fail_count: st === 1 ? ri(0, 3) : 0,
      fail_reason: st === 1 ? '网络超时' : null, ext_data: null,
      create_time: ago(ri(0, 25), ri(9, 22)), update_time: NOW,
      create_user: 0, update_user: 0, is_deleted: 0
    });
  }

  // ================= 15. invoice_title 15 =================
  const invoiceTitles = [
    ['瑞吉餐饮（北京）有限公司', '91110105MA01ABC123', 1],
    ['北京华信科技有限公司', '91110108MA01BDEF45', 1],
    ['北京智联信息技术有限公司', '91110108MA02GHIJ67', 1],
    ['北京博创软件有限公司', '91110108MA03KLMN89', 1],
    ['北京天辰科技有限公司', '91110108MA04OPQR01', 1],
    ['林晓', null, 2], ['张伟', null, 2], ['李伟', null, 2], ['陈芳', null, 2], ['赵磊', null, 2],
    ['孙丽', null, 2], ['周敏', null, 2], ['吴强', null, 2], ['郑涛', null, 2], ['刘杰', null, 2]
  ].map((t, i) => ({
    id: i + 1, title: t[0], tax_number: t[1], company_name: t[1],
    type: t[2], tenant_id: TID, user_id: pick(users).id,
    create_time: ago(ri(100, 300)), update_time: NOW
  }));

  // ================= 16. invoice_record 20 =================
  const invoiceRecords = orders.slice(0, 20).map((o, i) => {
    const t = pick(invoiceTitles);
    return {
      id: i + 1, order_id: o.id, user_id: o.uid,
      order_no: '2026' + pad2(o.t.getMonth() + 1) + pad2(o.t.getDate()) + String(100000 + o.id).slice(1),
      title_id: t.id, title: t.title, tax_number: t.tax_number,
      type: t.type, amount: o.amt,
      status: pick([1, 1, 1, 2, 3]),
      invoice_no: '12345678', invoice_code: null, invoice_url: null,
      apply_time: o.t, issue_time: pick([1, 2, 3]) === 3 ? new Date(o.t.getTime() + 3600000) : null,
      tenant_id: TID, create_time: o.t, update_time: NOW
    };
  });

  // ================= 17. cs_session 12 + cs_message 30 =================
  const csSessions = [];
  const agentNames = ['客服小李', '客服小王', '客服小张'];
  for (let i = 0; i < 12; i++) {
    const st = pick([1, 1, 2, 2, 3]);
    const t = ago(ri(0, 15), ri(9, 22));
    csSessions.push({
      id: i + 1, session_no: 'CS2026' + (100000 + i),
      user_id: pick(users).id, user_name: pick(users).name,
      agent_id: i + 1, agent_name: agentNames[i % 3],
      session_type: pick([1, 2]), order_id: pick(orders).id,
      status: st, first_response_time: new Date(t.getTime() + 60000),
      close_time: st === 3 ? new Date(t.getTime() + 3600000) : null,
      satisfaction_rating: st === 3 ? ri(3, 5) : null,
      user_feedback: st === 3 ? pick(['非常满意', '满意', '一般', null]) : null,
      tenant_id: TID, create_time: t, update_time: NOW
    });
  }
  const csMessages = [];
  const userMsgs = ['你好，我想问一下...', '我的订单什么时候到？', '这个菜品能不能换口味？', '我要投诉上次的问题', '优惠券能用吗？', '请问配送费怎么算的？'];
  const agentMsgs = ['您好，有什么可以帮您？', '好的，我帮您查询一下', '已为您处理，请稍等', '感谢您的反馈，我们会改进', '优惠券已为您查询，适用满减'];
  csSessions.forEach((s, i) => {
    const n = ri(3, 5);
    for (let j = 0; j < n; j++) {
      const isUser = j % 2 === 0;
      csMessages.push({
        id: csMessages.length + 1, session_id: s.id,
        sender_type: isUser ? 1 : 2, sender_id: isUser ? s.user_id : s.agent_id,
        sender_name: isUser ? s.user_name : s.agent_name,
        message_type: 1,
        content: isUser ? pick(userMsgs) : pick(agentMsgs),
        image_url: null, is_read: 1, tenant_id: TID,
        create_time: new Date(s.create_time.getTime() + j * 120000)
      });
    }
  });

  // ================= 18. complaint 10 =================
  const complaints = orders.slice(0, 10).map((o, i) => {
    const st = pick([1, 2, 2, 3, 3, 3]);
    return {
      id: i + 1, complaint_no: 'CP2026' + (100000 + i),
      user_id: o.uid, user_name: users.find(u => u.id === o.uid)?.name || '用户',
      user_phone: '138' + String(10000000 + o.uid * 31).slice(0, 8),
      order_id: o.id,
      order_number: '2026' + pad2(o.t.getMonth() + 1) + pad2(o.t.getDate()) + String(100000 + o.id).slice(1),
      complaint_type: pick([1, 2, 3, 4]),
      title: pick(['配送超时', '菜品质量问题', '服务态度问题', '包装破损']),
      content: pick(['配送时间过长，等了很久才收到', '菜品口味不佳，与描述不符', '外卖员态度不好', '餐盒破损，汤汁洒出']),
      image_urls: null, status: st,
      handler_id: 2, handler_name: '张建国',
      handle_result: st >= 3 ? pick(['已联系用户道歉', '已申请补偿优惠券', '已退款处理']) : null,
      compensation_amount: st >= 3 ? pick([0, 5, 10]) : 0,
      handle_time: st >= 3 ? new Date(o.t.getTime() + 7200000) : null,
      satisfaction: st === 3 ? ri(3, 5) : null,
      user_feedback: null, tenant_id: TID,
      create_time: new Date(o.t.getTime() + 3600000), update_time: NOW
    };
  });

  // ================= 19. platform_config 5 + dish_platform_mapping 40 =================
  const platforms = [
    ['WECHAT_MINIPROGRAM', '微信小程序', 'wx_app_123'],
    ['MEITUAN', '美团外卖', 'mt_shop_001'],
    ['ELEME', '饿了么', 'ele_shop_001'],
    ['DOUYIN', '抖音团购', 'dy_shop_001'],
    ['KA', '肯德基KFC', 'kf_shop_001']
  ].map((p, i) => ({
    id: i + 1, platform_type: p[0], platform_name: p[1],
    shop_id: p[2], app_key: null, app_secret: null,
    access_token: 'tk_' + (100000 + i).toString(16),
    enabled: i < 3 ? 1 : 0, sync_scope: 1, remark: null,
    tenant_id: TID, is_deleted: 0, create_time: ago(ri(200, 400)), update_time: NOW
  }));
  const dishMapping = [];
  for (let i = 0; i < 40; i++) {
    const d = dishes[i % dishes.length];
    const p = platforms[i % platforms.length];
    dishMapping.push({
      id: i + 1, dish_id: d.id, platform_type: p.platform_type,
      platform_shop_id: p.shop_id,
      platform_dish_id: 'p' + (100000 + i * 7),
      platform_sku_id: 'sku' + (100000 + i * 7),
      price: d.price, status: 1, tenant_id: TID,
      is_deleted: 0, create_time: ago(ri(100, 300)), update_time: NOW
    });
  }

  // ================= 20. platform_sync_log 20 + platform_reconcile_task 10 =================
  const syncLogs = [];
  const actions = ['CREATE', 'UPDATE', 'DELETE', 'SYNC'];
  for (let i = 0; i < 20; i++) {
    const p = pick(platforms);
    syncLogs.push({
      id: i + 1, tenant_id: TID, platform_type: p.platform_type,
      platform_order_id: 'p' + (100000 + i),
      local_order_id: pick(orders).id,
      action: pick([1, 2, 3, 4]), direction: pick([1, 2]),
      request_body: JSON.stringify({ action: pick([1, 2, 3, 4]), dish: dishes[ri(0, dishes.length - 1)].id }),
      response_body: JSON.stringify({ code: 0, msg: 'ok' }),
      status: rnd() < 0.9 ? 1 : 2,
      error_message: rnd() < 0.1 ? '平台接口超时' : null,
      retry_count: rnd() < 0.3 ? ri(1, 3) : 0,
      create_time: ago(ri(0, 20), ri(9, 22))
    });
  }
  const reconcileTasks = [];
  // 唯一键 (reconcile_date, platform_type, tenant_id)：用固定日期避免冲突
  const reconcilePlatforms = ['WECHAT_MINIPROGRAM', 'MEITUAN', 'ELEME', 'DOUYIN', 'KA'];
  for (let i = 0; i < 15; i++) {
    const d = ago(i, 10);   // 每天 1 条，跨 15 天，每平台 3 天
    const plat = reconcilePlatforms[i % 5];
    const total = ri(50, 120);
    const match = total - ri(0, 5);
    reconcileTasks.push({
      id: i + 1, tenant_id: TID, platform_type: plat,
      reconcile_date: new Date(d.getFullYear(), d.getMonth(), d.getDate()),
      begin_time: new Date(d.getFullYear(), d.getMonth(), d.getDate(), 0),
      end_time: new Date(d.getFullYear(), d.getMonth(), d.getDate(), 23, 59),
      total_platform_count: total, total_local_count: total,
      match_count: match,
      missing_local_count: total - match,
      missing_platform_count: total - match,
      status: match === total ? 2 : 1,
      error_message: match === total ? null : '存在差异订单',
      create_time: d, update_time: NOW
    });
  }

  // ================= 21. printer_config 12 + print_terminal 12 + print_task 20 + printer_log 20 =================
  const printers = [
    ['前台主打印', 'EPSON', 'TM-T88V'], ['前台副打印', 'EPSON', 'TM-T88V'],
    ['后厨热菜打印', 'Xprinter', 'XP-365B'], ['后厨凉菜打印', 'Xprinter', 'XP-365B'],
    ['后厨面点打印', 'Gprinter', 'GP-3120T'], ['后厨饮品打印', 'Gprinter', 'GP-3120T'],
    ['收银小票打印', 'EPSON', 'TM-T88V'], ['备餐打印', 'Xprinter', 'XP-365B'],
    ['打包打印', 'Xprinter', 'XP-365B'], ['会员通知打印', 'EPSON', 'TM-T88V'],
    ['标签打印', 'Gprinter', 'GP-3120T'], ['备用打印', 'EPSON', 'TM-T88V']
  ].map((p, i) => ({
    id: i + 1, tenant_id: TID, store_id: i < 6 ? 1 : 2,
    name: p[0], type: i < 8 ? 1 : 2,
    brand: p[1], device_id: 'dev_' + (100000 + i * 31),
    system_printer_name: p[1] + '_' + pad2(i + 1),
    ip_address: i < 8 ? '192.168.1.' + (10 + i) : null,
    port: 9100, paper_size: i < 8 ? 58 : 80,
    print_types: JSON.stringify([i < 6 ? 1 : (i < 8 ? 2 : 3)]),
    status: i < 10 ? 1 : 0, sort: i + 1,
    create_time: ago(ri(200, 400)), update_time: NOW,
    is_deleted: 0, create_user: 1, update_user: 1
  }));
  const terminals = printers.slice(0, 12).map((p, i) => ({
    id: i + 1, tenant_id: TID,
    store_code: 'STORE' + String(1 + (i < 6 ? 0 : 1)).padStart(3, '0'),
    terminal_code: 'TERM' + pad2(i + 1).padStart(3, '0'),
    token: 'tk_term_' + (100000 + i).toString(16),
    name: p.name, printer_name: p.name,
    paper_size: p.paper_size,
    print_types: p.print_types,
    status: i < 10 ? 1 : 0,
    last_heartbeat: ago(ri(0, 5), ri(9, 22)),
    client_version: '1.2.0',
    create_time: ago(ri(200, 400)), update_time: NOW, is_deleted: 0
  }));
  const printTasks = [];
  for (let i = 0; i < 20; i++) {
    const ord = pick(orders);
    const t = terminals[i % terminals.length];
    printTasks.push({
      id: i + 1, tenant_id: TID,
      store_code: t.store_code, order_id: ord.id,
      task_type: pick([1, 2, 3]),
      content: JSON.stringify({ items: ['红烧肉x1', '米饭x1'], total: ord.amt }),
      status: pick([1, 1, 1, 2, 3]),
      terminal_id: t.id, terminal_code: t.terminal_code,
      error_msg: '', retry_count: 0,
      created_time: ord.t, pulled_time: new Date(ord.t.getTime() + 30000),
      done_time: new Date(ord.t.getTime() + 60000)
    });
  }
  const printerLogs = printTasks.map((t, i) => ({
    id: i + 1, order_id: t.order_id,
    print_type: t.task_type, printer_id: t.terminal_id,
    content: JSON.stringify({ task_id: t.id }),
    status: t.status, error_msg: '',
    created_time: t.created_time, is_deleted: 0,
    create_user: 0, update_time: NOW, update_user: 0, tenant_id: TID
  }));

  // ================= 22. operation_log 40 + log 20 =================
  const opLogs = [];
  const opModules = ['菜品管理', '订单管理', '用户管理', '营销活动', '库存管理', '财务结算', '系统设置', '报表', '客服管理', '打印管理'];
  const opTypes = ['CREATE', 'UPDATE', 'DELETE', 'QUERY', 'LOGIN', 'LOGOUT', 'EXPORT', 'IMPORT'];
  for (let i = 0; i < 40; i++) {
    const e = pick(employees);
    opLogs.push({
      id: i + 1, operator_id: e.id, operator_name: e.name,
      operator_ip: '10.0.0.' + ri(10, 250),
      module: pick(opModules), operation_type: pick(opTypes),
      table_name: pick(['dish', 'orders', 'user', 'coupon_template', 'material', 'daily_settlement', 'system_config']),
      biz_id: ri(1, 100),
      description: pick(['新增菜品', '修改订单状态', '查看用户详情', '创建活动', '入库操作', '查看报表', '修改配置', '导出数据']),
      old_value: null, new_value: null,
      request_url: '/api/v1/' + pick(['dish', 'order', 'user', 'coupon', 'material']),
      request_method: pick(['GET', 'POST', 'PUT', 'DELETE']),
      request_params: JSON.stringify({ id: ri(1, 100) }),
      duration: ri(20, 800),
      is_success: 1, error_msg: null,
      tenant_id: TID, create_time: ago(ri(0, 15), ri(9, 22)), is_deleted: 0
    });
  }
  const logs = opLogs.slice(0, 20).map((o, i) => ({
    id: i + 1, operate_user: o.operator_id, operate_name: o.operator_name,
    module: o.module, type: o.operation_type,
    method: o.request_method, request_url: o.request_url,
    request_params: o.request_params, response_data: null,
    ip: o.operator_ip, status: o.is_success, error_msg: null,
    cost_time: o.duration, create_time: o.create_time,
    is_deleted: 0, tenant_id: TID
  }));

  // ================= 写入 =================
  const TABS = [
    ['tenant', tenants], ['store', stores], ['store_info', storeInfo],
    ['role', roles], ['menu', allMenus], ['permission', perms],
    ['role_permission', rolePerms], ['employee', employees],
    ['employee_role', empRoles], ['system_config', configs],
    ['ai_prompt_template', aiPrompts], ['ai_provider_config', aiProviders],
    ['notification_template', notifyTemplates], ['notification_record', notifyRecords],
    ['invoice_title', invoiceTitles], ['invoice_record', invoiceRecords],
    ['cs_session', csSessions], ['cs_message', csMessages],
    ['complaint', complaints], ['platform_config', platforms],
    ['dish_platform_mapping', dishMapping], ['platform_sync_log', syncLogs],
    ['platform_reconcile_task', reconcileTasks],
    ['printer_config', printers], ['print_terminal', terminals],
    ['print_task', printTasks], ['printer_log', printerLogs],
    ['operation_log', opLogs], ['log', logs]
  ];
  const esc = v => {
    if (v === null || v === undefined) return 'NULL';
    if (v instanceof Date) return "'" + dt(v) + "'";
    if (typeof v === 'boolean') return v ? '1' : '0';
    if (typeof v === 'number') return String(v);
    return "'" + String(v).replace(/\\/g, '\\\\').replace(/'/g, "''") + "'";
  };
  const parts = ['SET NAMES utf8mb4;'];
  for (const [t, rows] of TABS) {
    const cols = (await q(`SHOW COLUMNS FROM \`${t}\``)).map(r => r.Field);
    await c.query(`DELETE FROM \`${t}\``);
    const sql = `INSERT INTO \`${t}\` (${cols.map(x => '`' + x + '`').join(',')}) VALUES ${rows.map(r => '(' + cols.map(x => esc(r[x])).join(',') + ')').join(',\n')};`;
    await c.query(sql);
    parts.push(`-- ${t}: ${rows.length}`); parts.push(`DELETE FROM \`${t}\`;`); parts.push(sql);
    console.log(`  [OK] ${t.padEnd(26)} ${rows.length} 行`);
  }
  await c.end();
  fs.writeFileSync(OUT, parts.join('\n'), 'utf8');
  console.log(`\n[SQL 落盘] ${OUT}`);
})().catch(e => { console.error('FAIL:', e.message); process.exit(1); });
