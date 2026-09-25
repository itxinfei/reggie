// domain-x-extra.js — 补齐不足 10 行的零散表（ai_provider_config / platform_config / refund_record）
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');
const OUT = path.join(__dirname, '..', 'src/main/resources/db/seed/domain-x-extra.sql');

let _s = 73737;
const rnd = () => { _s = (_s * 1103515245 + 12345) & 0x7fffffff; return _s / 0x7fffffff; };
const ri = (a, b) => a + Math.floor(rnd() * (b - a + 1));
const pick = a => a[Math.floor(rnd() * a.length)];
const pad2 = n => String(n).padStart(2, '0');
const dt = d => d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate()) + ' ' + pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ':' + pad2(d.getSeconds());

(async () => {
  const c = await mysql.createConnection({ host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'reggie' });
  const q = async s => { const [r] = await c.query(s); return r; };
  const TID = 1;
  const NOW = new Date(2026, 8, 25, 18, 0, 0);
  const ago = (d, h) => { const x = new Date(NOW); x.setDate(x.getDate() - d); x.setHours(h || ri(9, 22), ri(0, 59), 0, 0); return x; };

  // ===== 1. ai_provider_config 5 -> 12 =====
  const providers = [
    ['deepseek', 'DeepSeek', 'https://api.deepseek.com/v1', 'deepseek-chat', 'b81e2'],
    ['openai', 'OpenAI', 'https://api.openai.com/v1', 'gpt-4o-mini', 'sk-proj'],
    ['moonshot', 'Kimi(月之暗面)', 'https://api.moonshot.cn/v1', 'moonshot-v1-8k', 'sk-moon'],
    ['zhipu', '智谱GLM', 'https://open.bigmodel.cn/api/paas/v4', 'glm-4-flash', 'gl-4f'],
    ['qianfan', '百度文心', 'https://qianfan.bj.baidubce.com/v2', 'ernie-4.0-tiny-8k', 'ern-4t'],
    ['dashscope', '阿里通义千问', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'qwen-turbo', 'sk-dash'],
    ['baichuan', '百川智能', 'https://api.baichuan-ai.com/v1', 'baichuan2-turbo', 'bc-turbo'],
    ['yi', '零一万物', 'https://api.lingyiwanwu.com/v1', 'yi-lightning', 'yi-lite'],
    ['step', '阶跃星辰', 'https://api.stepfun.com/v1', 'step-2-16k', 'st-2-16k'],
    ['minimax', 'MiniMax', 'https://api.minimax.chat/v1', 'abab6.5s-chat', 'mm-65s'],
    ['local-bge', '本地BGE向量模型', 'http://192.168.1.10:8001/v1', 'bge-large-zh-v1.5', 'local-key'],
    ['mistral', 'Mistral', 'https://api.mistral.ai/v1', 'mistral-small-latest', 'ms-sml']
  ].map((p, i) => ({
    id: i + 1, provider_code: p[0], provider_name: p[1], base_url: p[2],
    model_name: p[3], api_key: p[4] + 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',
    timeout: p[0] === 'local-bge' ? 120 : 60,
    max_tokens: p[0] === 'local-bge' ? 8192 : 4096,
    temperature: +((0.3 + rnd() * 0.6).toFixed(2)),
    api_format: 'openai',
    extra_headers: JSON.stringify({ 'User-Agent': 'reggie/1.0' }),
    request_template: null, response_path: 'choices.0.message.content',
    icon_url: null, enabled: 1, is_active: i < 6 ? 1 : 0,
    last_test_time: ago(ri(0, 10)),
    last_test_result: p[0] === 'mistral' ? '失败: 需订阅套餐' : '成功',
    sort: i + 1,
    remark: p[0] === 'local-bge' ? '向量嵌入专用，1536维' : null,
    create_time: ago(ri(10, 60), ri(9, 22)), update_time: NOW,
    create_user: 1, update_user: 1, is_deleted: 0,
    capabilities: JSON.stringify(p[0] === 'local-bge' ? ['embedding'] : ['chat']),
    embedding_dimensions: p[0] === 'local-bge' ? 1536 : null
  }));

  // ===== 2. platform_config 5 -> 12 =====
  const platforms = [
    ['meituan_waimai', '美团外卖', '88001234', 'mt_a91'],
    ['eleme', '饿了么', 'ELM_556677', 'el_b23'],
    ['jd_takeout', '京东到家', 'JD_778899', 'jd_c34'],
    ['douyin_group', '抖音团购', 'DY_112233', 'dy_d45'],
    ['kuaishou_group', '快手团购', 'KS_334455', 'ks_e56'],
    ['meituan_kandan', '美团看店宝', '88001234', 'mt_f67'],
    ['eleme_dinner', '饿了么到店', 'ELM_556677', 'el_g78'],
    ['wechat_store', '微信视频号小店', 'WX_667788', 'wx_h89'],
    ['xiaohongshu', '小红书团购', 'XHS_889900', 'xh_i90'],
    ['baidu_waimai', '百度外卖', 'BD_990011', 'bd_j01'],
    ['pinduoduo', '拼多多小时达', 'PDD_112234', 'pd_k12'],
    ['pos_local', '本地POS收银', 'POS-STORE-01', 'pos_l34']
  ].map((p, i) => ({
    id: i + 1, platform_type: p[0], platform_name: p[1], shop_id: p[2],
    app_key: p[3] + 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',
    app_secret: p[3].toUpperCase() + 'YYYYYYYYYYYYYYYYYYYYYYYY',
    access_token: 'tk_' + (20260900 + i) + 'abcdef',
    enabled: i < 7 ? 1 : 0, sync_scope: 1,
    remark: null, tenant_id: TID, is_deleted: 0,
    create_time: ago(ri(20, 90), ri(9, 22)), update_time: NOW
  }));

  // ===== 3. refund_record 2 -> 18 =====
  const pos = (await q('SELECT id,order_id,amount,tenant_id FROM payment_order WHERE is_deleted=0 ORDER BY id')).map(r => ({
    id: +r.id, oid: +r.order_id, amt: +r.amount, tid: +r.tenant_id
  }));
  const reasons = ['餐品有异物', '配送超时严重', '少送餐品', '餐品未加热', '口味与描述不符', '包装破损洒漏', '错送餐品', '骑手态度差', '餐品份量不足', '变质发霉', '与图片严重不符', '漏送餐具', '金额重复扣款', '用户改主意', '下单错误', '地址填写有误', '联系不上用户', '用户要求取消'];
  const rr = pos.slice(0, 18).map((p, i) => {
    const partial = i % 3 === 1;
    const amt = partial ? +((p.amt * (0.3 + rnd() * 0.4)).toFixed(2)) : p.amt;
    const st = pick([1, 2, 2, 3, 3, 4]);
    const t = ago(ri(0, 20), ri(10, 21));
    return {
      id: i + 1, payment_order_id: p.id, order_id: p.oid, tenant_id: p.tid,
      refund_no: 'RF' + (2026090000000 + i * 131),
      amount: amt, reason: reasons[i],
      status: String(st),
      refund_type: partial ? 2 : 1,
      apply_user_id: 1, created_time: t, is_deleted: 0,
      version: st >= 2 ? 2 : 1,
      create_user: 1, update_time: NOW, update_user: 1,
      audit_user_id: st >= 2 ? 1 : null,
      audit_time: st >= 2 ? new Date(t.getTime() + 1800000) : null,
      reject_reason: st === 4 ? '订单已出餐完成，不符合退款条件' : null,
      refund_time: st === 3 ? new Date(t.getTime() + 3600000) : null
    };
  });

  // ===== 写入 =====
  const TABS = [['ai_provider_config', providers], ['platform_config', platforms], ['refund_record', rr]];
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
    parts.push('-- ' + t + ': ' + rows.length); parts.push('DELETE FROM `' + t + '`;'); parts.push(sql);
    console.log('  [OK] ' + t.padEnd(22) + ' ' + rows.length + ' 行');
  }
  await c.end();
  fs.writeFileSync(OUT, parts.join('\n'), 'utf8');
  console.log('\n[SQL 落盘] ' + OUT);
})().catch(e => { console.error('FAIL:', e.message); process.exit(1); });
