// domain-i-ai.js — AI 域(对话/消息/附件/知识库/用户画像/推荐)
// 用法: node scripts/domain-i-ai.js
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');
const OUT = path.join(__dirname, '..', 'src/main/resources/db/seed/domain-i-ai.sql');

let _s = 424242;
const rnd = () => { _s = (_s * 1103515245 + 12345) & 0x7fffffff; return _s / 0x7fffffff; };
const ri = (a, b) => a + Math.floor(rnd() * (b - a + 1));
const pick = a => a[Math.floor(rnd() * a.length)];
const rf = (a, b, d = 2) => +((a + rnd() * (b - a)).toFixed(d));
const pad2 = n => String(n).padStart(2, '0');
const dt = d => d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate()) + ' ' + pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ':' + pad2(d.getSeconds());
const pad8 = n => String(n).padStart(8, '0');
// 模拟 embedding 向量（1536 维，用 JSON 数组的简短形式）
function fakeEmb(seed, dim = 1536) {
  let s = seed;
  const arr = new Array(dim);
  for (let i = 0; i < dim; i++) {
    s = (s * 1103515245 + 12345) & 0x7fffffff;
    arr[i] = ((s % 20000) / 10000 - 1).toFixed(4);
  }
  return '[' + arr.join(',') + ']';
}
const sha = s => {
  let h1 = 0x811c9dc5, h2 = 0xdeadbeef;
  for (let i = 0; i < s.length; i++) {
    h1 = (h1 ^ s.charCodeAt(i)) >>> 0; h1 = (h1 * 16777619) >>> 0;
    h2 = ((h2 << 5) + h2 + s.charCodeAt(i)) >>> 0;
  }
  return (h1.toString(16) + h2.toString(16)).padEnd(64, '0').slice(0, 64);
};

(async () => {
  const c = await mysql.createConnection({ host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'reggie', charset: 'utf8mb4' });
  const q = async s => { const [r] = await c.query(s); return r; };
  const TID = 1;
  const NOW = new Date(2026, 8, 25, 18, 0, 0);
  const ago = (d, h) => { const x = new Date(NOW); x.setDate(x.getDate() - d); x.setHours(h || ri(9, 22), ri(0, 59), 0, 0); return x; };
  const later = (d, h) => { const x = new Date(NOW); x.setDate(x.getDate() + d); x.setHours(h || 12, 0, 0, 0); return x; };

  const users = (await q('SELECT id,name FROM user ORDER BY id')).map(r => ({ id: +r.id, name: r.name }));
  const dishes = (await q('SELECT id,name,price FROM dish WHERE is_deleted=0 ORDER BY id')).map(r => ({ id: +r.id, name: r.name, price: +r.price }));
  console.log(`引用: user=${users.length} dish=${dishes.length}`);

  const scenes = ['recommend', 'complaint', 'order_confirm', 'ingredient_q', 'nutrition', 'promo_explain', 'delivery_q', 'allergic_advice', 'member_service', 'default'];
  const sceneNames = { recommend: '智能点餐', complaint: '投诉处理', order_confirm: '订单确认', ingredient_q: '菜品咨询', nutrition: '营养咨询', promo_explain: '优惠说明', delivery_q: '配送咨询', allergic_advice: '过敏咨询', member_service: '会员服务', default: '默认' };

  // 用户常用问句与 AI 回复模板（按场景）
  const convoSeeds = [
    ['推荐几个适合加班的快食', 'recommend', '为您推荐适合加班的快手餐：扬州炒饭、番茄蛋花汤、宫保鸡丁，15分钟即可送达。'],
    ['我的订单为什么还没到？', 'complaint', '已为您查询订单配送状态，当前骑手正在派送中，预计10分钟内送达。'],
    ['红烧肉怎么做的？', 'ingredient_q', '红烧肉是经典中式菜肴，采用五花肉慢炖2小时，配以冰糖、酱油、葱姜等。'],
    ['这个套餐包含哪些菜？', 'order_confirm', '该套餐包含主食1份、荤菜2份、素菜1份和汤品1份，详见套餐明细。'],
    ['我过敏的食物能避开吗？', 'allergic_advice', '已根据您的过敏信息（花生、海鲜）自动过滤相关菜品，剩余48道可选。'],
    ['有什么优惠券可用？', 'promo_explain', '您当前有3张可用券：满50减10、满30减5、全场85折，可在结算时使用。'],
    ['配送费怎么算的？', 'delivery_q', '配送费基于距离阶梯计费，3公里内4元，每增加1公里加1.5元。'],
    ['这个菜的热量是多少？', 'nutrition', '红烧肉每100克约含250千卡，主要成分为脂肪和蛋白质，适量食用。'],
    ['会员积分能兑换什么？', 'member_service', '积分可兑换优惠券、菜品兑换券或积分抵现，100积分抵1元。'],
    ['请推荐辣味菜品', 'recommend', '推荐辣味菜品：辣子鸡、毛血旺、宫保鸡丁、鱼香肉丝，您常点的辣子鸡也在列。']
  ];
  const userMsgs = ['你好', '有没有推荐的？', '这个多少钱？', '可以加辣吗？', '怎么取消订单？', '配送到哪里？', '开发票吗？', '谢谢'];
  const agentReplies = ['您好，有什么可以帮您？', '好的，帮您查询', '已为您处理，请稍等', '感谢您的反馈', '您可以这样操作...', '还有其他问题吗？', '请确认信息', '感谢您的支持'];

  // ============ AI 对话 20 条 + 消息 60 条 ============
  const convs = [];
  const msgs = [];
  for (let i = 0; i < 20; i++) {
    const u = pick(users);
    const seed = convoSeeds[i % convoSeeds.length];
    const scene = seed[1];
    const cid = 'conv_' + pad8(20260900 + i);
    const t0 = ago(ri(0, 15), ri(9, 22));
    const msgCount = ri(2, 4);
    convs.push({
      id: i + 1, conversation_id: cid, user_id: u.id, actor_type: 'user',
      title: seed[0].slice(0, 20) || (sceneNames[scene] || 'AI 对话'),
      scene, message_count: msgCount, is_deleted: 0,
      create_user: 0, update_user: 0,
      create_time: t0, update_time: new Date(t0.getTime() + msgCount * 60000),
      tenant_id: TID
    });
    for (let j = 0; j < msgCount; j++) {
      const isUser = j % 2 === 0;
      const ctime = new Date(t0.getTime() + j * 90000);
      const content = isUser ? (j === 0 ? seed[0] : pick(userMsgs)) : (j === 1 ? seed[2] : pick(agentReplies));
      msgs.push({
        id: msgs.length + 1, conversation_id: cid, user_id: u.id,
        role: isUser ? 'user' : 'assistant',
        content,
        attachments: null,
        message_type: isUser ? 'text' : 'text',
        status: 'completed',
        client_msg_id: 'msg_' + pad8(3000000 + msgs.length),
        feedback: isUser ? null : (rnd() < 0.3 ? pick(['like', 'dislike']) : null),
        dish_ids: isUser ? null : JSON.stringify([pick(dishes).id, pick(dishes).id]),
        tokens_used: ri(50, 400),
        is_deleted: 0, update_time: ctime, create_user: 0, update_user: 0,
        create_time: ctime, tenant_id: TID
      });
    }
  }

  // ============ AI 附件 15 条 ============
  const attachments = [];
  const imgNames = ['menu_photo.jpg', 'dish_closeup.jpg', 'receipt.jpg', 'order_photo.jpg', 'feedback_img.jpg', 'allergen_info.jpg', 'coupon_scan.jpg'];
  const fileTypes = [
    ['jpg', 'image/jpeg'], ['png', 'image/png'], ['pdf', 'application/pdf'], ['txt', 'text/plain']
  ];
  for (let i = 0; i < 15; i++) {
    const u = pick(users);
    const ft = fileTypes[i % fileTypes.length];
    const fname = pick(imgNames).replace(/\.\w+$/, '') + '_' + i + '.' + ft[0];
    const w = ft[0] === 'jpg' || ft[0] === 'png' ? ri(600, 1920) : null;
    const h = ft[0] === 'jpg' || ft[0] === 'png' ? ri(600, 1920) : null;
    const fsize = ri(50000, 5000000);
    const storagePath = '/data/uploads/ai/attachments/' + (20260900 + i) + '/' + fname;
    attachments.push({
      id: i + 1, tenant_id: TID, actor_type: 'user', owner_id: u.id,
      scene: pick(scenes), file_name: fname, original_name: fname,
      content_type: ft[1], file_size: fsize, width: w, height: h,
      sha256: sha(fname + i), storage_path: storagePath,
      is_deleted: 0, create_time: ago(ri(0, 15), ri(9, 22)),
      update_time: NOW, create_user: 0, update_user: 0
    });
  }

  // ============ AI 知识库文档 15 条 ============
  const docTypes = ['menu', 'faq', 'policy', 'announcement'];
  const audiences = ['user', 'agent', 'admin'];
  const docTitles = [
    '瑞吉外卖完整菜单说明', '菜品口味与做法说明', '配送范围与时效政策',
    '优惠活动规则说明', '会员积分使用规则', '投诉与退款政策',
    '食材过敏原信息', '食品安全管理规定', '新用户引导说明',
    '常见问题解答FAQ', '节日促销活动预告', '配送异常处理流程',
    '发票开具指南', '隐私政策说明', '平台服务条款'
  ];
  const docs = docTitles.map((t, i) => ({
    id: i + 1, tenant_id: TID, title: t,
    doc_type: docTypes[i % docTypes.length],
    audience: audiences[i % audiences.length],
    content: '【' + t + '】\n\n' + (i % 4 === 0
      ? '这是瑞吉外卖官方说明文档，包含详细的规则、流程和操作指南。'
      : '本文档介绍' + t + '相关内容，供用户、客服或管理员参考。\n\n详细说明：\n1. 适用范围：全部用户/订单/菜品\n2. 生效时间：2026年9月1日\n3. 如有疑问请联系客服。') + '\n\n更新记录：' + ago(ri(0, 60)).toDateString(),
    status: 'published', chunk_count: ri(3, 6),
    error_msg: null,
    create_time: ago(ri(0, 60), ri(9, 22)), update_time: NOW,
    create_user: 1, update_user: 1, is_deleted: 0
  }));

  // ============ AI 知识分块 60 条 ============
  const chunks = [];
  const chunkContents = [
    '红烧肉采用五花肉慢炖2小时，配料含冰糖、酱油、葱姜，主要成分脂肪与蛋白质。',
    '配送范围：门店周边5公里内，3公里内配送费4元，每增加1公里加1.5元。',
    '用户取消订单需在15分钟内申请，超时订单进入备餐流程后不可取消。',
    '会员等级：普通会员、银牌、金牌、钻石，等级越高积分抵扣比例越高。',
    '优惠活动：周末满50减10，工作日满30减5，新用户专享满20减8。',
    '发票申请：用户在订单完成后24小时内可申请电子发票，默认开具普通发票。',
    '过敏原说明：菜品均标注是否含花生、海鲜、坚果、乳制品等主要过敏原。',
    '投诉处理：客服将在24小时内响应，重大投诉48小时内给予书面回复。',
    '退款政策：未出餐订单可全额退款，已出餐订单按实际损失比例退款。',
    '食品安全：所有食材来源可追溯，后厨每日清洁，厨师持证上岗。',
    '用户隐私：平台严格遵守个人信息保护法，不向第三方共享用户数据。',
    '服务条款：用户下单即视为同意平台服务条款与用户协议。',
    '配送异常处理：超时配送可申请补偿，连续超时将自动赔付优惠券。',
    '菜单更新：菜单每月更新一次，节假日推出应节菜品。',
    '配送时效：一般30-45分钟送达，高峰期可能延长至60分钟。'
  ];
  for (let i = 0; i < 60; i++) {
    const doc = docs[i % docs.length];
    chunks.push({
      id: i + 1, tenant_id: TID, doc_id: doc.id,
      chunk_index: (i % 6) + 1,
      content: chunkContents[i % chunkContents.length],
      embedding: fakeEmb(i * 7919, 1536),
      embed_model: 'bge-large-zh-v1.5',
      create_time: ago(ri(0, 30), ri(9, 22)),
      update_time: NOW
    });
  }

  // ============ AI 用户画像 20 条 ============
  const tasteTags = ['辣', '微辣', '清淡', '重口味', '甜', '酸', '咸', '香', '辣子鸡', '麻辣香锅'];
  const catTags = ['荤菜', '素菜', '主食', '饮品', '甜品', '凉菜', '套餐', '素食', '轻食'];
  const dislikeTags = ['太辣', '油大', '太甜', '价格高', '配送慢', '分量小', '口味淡'];
  const allergiesList = ['花生', '海鲜', '坚果', '乳制品', '鸡蛋', '麸质', '大豆'];
  const pricePrefs = ['low', 'medium', 'high'];
  const diningTypes = ['dine_in', 'takeaway', 'delivery'];
  const timeSlots = ['breakfast', 'lunch', 'dinner', 'late_night'];

  const profiles = users.slice(0, 20).map((u, i) => ({
    id: i + 1, user_id: u.id, tenant_id: TID,
    taste_tags: JSON.stringify([pick(tasteTags), pick(tasteTags), pick(tasteTags)].filter((v, idx, a) => a.indexOf(v) === idx)),
    category_tags: JSON.stringify([pick(catTags), pick(catTags), pick(catTags)].filter((v, idx, a) => a.indexOf(v) === idx)),
    disliked_tags: JSON.stringify([pick(dislikeTags), pick(dislikeTags)]),
    allergies: JSON.stringify([allergiesList[ri(0, allergiesList.length - 1)]]),
    price_preference: pick(pricePrefs),
    avg_order_amount: ri(30, 120),
    usual_diners: ri(1, 4),
    user_tags: JSON.stringify([pick(['高频', '新客', '复购', '会员', '沉睡'])]),
    frequent_dish_ids: JSON.stringify([pick(dishes).id, pick(dishes).id, pick(dishes).id].filter((v, idx, a) => a.indexOf(v) === idx)),
    preferred_dining_type: pick(diningTypes),
    preferred_time_slot: pick(timeSlots),
    delivery_fee_sensitive: rnd() < 0.5 ? 1 : 0,
    confidence: rf(0.55, 0.98, 2),
    last_analyzed_time: ago(ri(0, 10)),
    total_conversations: ri(5, 50),
    total_feedbacks: ri(0, 10),
    create_time: ago(ri(10, 60), ri(9, 22)),
    update_time: NOW,
    create_user: 0, update_user: 0, is_deleted: 0
  }));

  // ============ 推荐缓存 20 条 ============
  const recTypes = ['top', 'similar', 'personalized'];
  const algoNames = ['cf', 'content', 'hybrid', 'popular'];
  const recCaches = users.slice(0, 20).map((u, i) => {
    const picks = [pick(dishes).id, pick(dishes).id, pick(dishes).id, pick(dishes).id, pick(dishes).id];
    const uniq = picks.filter((v, idx, a) => a.indexOf(v) === idx);
    return {
      id: i + 1, user_id: u.id, tenant_id: TID,
      recommend_type: ri(1, 3),
      dish_ids: JSON.stringify(uniq),
      algo_name: pick(algoNames),
      score: rf(0.6, 0.99, 2),
      expire_time: later(ri(0, 3), 23),
      create_time: ago(ri(0, 3), ri(9, 22)),
      update_time: NOW,
      is_deleted: 0, create_user: 0, update_user: 0
    };
  });

  // ============ 推荐反馈 25 条 ============
  const recFeedbacks = [];
  for (let i = 0; i < 25; i++) {
    const rc = pick(recCaches);
    recFeedbacks.push({
      id: i + 1, user_id: rc.user_id, tenant_id: TID,
      recommend_cache_id: rc.id,
      dish_id: pick(dishes).id,
      feedback_type: pick([1, 1, 2, 3]),   // 1点击 2购买 3不感兴趣
      create_time: ago(ri(0, 5), ri(9, 22)),
      is_deleted: 0, create_user: 0, update_time: NOW, update_user: 0
    });
  }

  // ================= 写入 =================
  const TABS = [
    ['ai_conversation', convs], ['ai_message', msgs], ['ai_attachment', attachments],
    ['ai_knowledge_doc', docs], ['ai_knowledge_chunk', chunks],
    ['ai_user_profile', profiles], ['recommendation_cache', recCaches],
    ['recommendation_feedback', recFeedbacks]
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
