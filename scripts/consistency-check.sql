SET NAMES utf8mb4;
SELECT '== 1. 全库表数 ==' AS sec;
SELECT COUNT(*) AS tables_cnt FROM information_schema.tables WHERE table_schema='reggie';

SELECT '== 2. 金额自洽 ==' AS sec;
SELECT COUNT(*) AS bad_orders FROM (
  SELECT o.id, o.amount - (COALESCE(d.sa,0) + o.delivery_fee - o.full_reduction_amount - o.new_customer_discount_amount) AS diff
  FROM orders o
  LEFT JOIN (SELECT order_id, SUM(amount) sa FROM order_detail GROUP BY order_id) d ON d.order_id=o.id
  WHERE o.is_deleted=0
) x WHERE ABS(diff) > 30;

SELECT '== 3. 孤儿引用 ==' AS sec;
SELECT 'order_detail->orders' AS chk, COUNT(*) AS orphans FROM order_detail od LEFT JOIN orders o ON o.id=od.order_id WHERE o.id IS NULL;
SELECT 'payment_order->orders', COUNT(*) FROM payment_order po LEFT JOIN orders o ON o.id=po.order_id WHERE o.id IS NULL;
SELECT 'refund_record->payment_order', COUNT(*) FROM refund_record rr LEFT JOIN payment_order po ON po.id=rr.payment_order_id WHERE po.id IS NULL;
SELECT 'delivery_order->orders', COUNT(*) FROM delivery_order dr LEFT JOIN orders o ON o.id=dr.order_id WHERE o.id IS NULL;
SELECT 'order_detail->dish', COUNT(*) FROM order_detail od LEFT JOIN dish d ON d.id=od.dish_id WHERE od.dish_id IS NOT NULL AND d.id IS NULL;
SELECT 'coupon_user->coupon_template', COUNT(*) FROM coupon_user cu LEFT JOIN coupon_template ct ON ct.id=cu.template_id WHERE ct.id IS NULL;
SELECT 'campaign_usage_record->marketing_campaign', COUNT(*) FROM campaign_usage_record u LEFT JOIN marketing_campaign m ON m.id=u.campaign_id WHERE m.id IS NULL;
SELECT 'purchase_order_detail->material', COUNT(*) FROM purchase_order_detail d LEFT JOIN material m ON m.ID=d.MATERIAL_ID WHERE m.ID IS NULL;
SELECT 'stock_check_detail->material', COUNT(*) FROM stock_check_detail d LEFT JOIN material m ON m.ID=d.MATERIAL_ID WHERE m.ID IS NULL;
SELECT 'attendance->employee', COUNT(*) FROM attendance a LEFT JOIN employee e ON e.id=a.employee_id WHERE e.id IS NULL;
SELECT 'work_schedule->employee', COUNT(*) FROM work_schedule w LEFT JOIN employee e ON e.id=w.employee_id WHERE e.id IS NULL;
SELECT 'ai_message->ai_conversation', COUNT(*) FROM ai_message m LEFT JOIN ai_conversation cv ON cv.conversation_id=m.conversation_id WHERE cv.conversation_id IS NULL;
SELECT 'ai_knowledge_chunk->doc', COUNT(*) FROM ai_knowledge_chunk k LEFT JOIN ai_knowledge_doc d ON d.id=k.doc_id WHERE k.doc_id IS NULL;
SELECT 'recommendation_cache->user', COUNT(*) FROM recommendation_cache rc LEFT JOIN user u ON u.id=rc.user_id WHERE u.id IS NULL;
SELECT 'dish->category', COUNT(*) FROM dish d LEFT JOIN category c ON c.id=d.category_id WHERE c.id IS NULL;
SELECT 'setmeal->category', COUNT(*) FROM setmeal s LEFT JOIN category c ON c.id=s.category_id WHERE c.id IS NULL;
SELECT 'group_buy_participation->campaign', COUNT(*) FROM group_buy_participation p LEFT JOIN group_buy_campaign g ON g.ID=p.GROUP_BUY_ID WHERE g.ID IS NULL;
SELECT 'ai_user_profile->user', COUNT(*) FROM ai_user_profile p LEFT JOIN user u ON u.id=p.user_id WHERE u.id IS NULL;
SELECT 'points_record->member', COUNT(*) FROM points_record p LEFT JOIN member m ON m.id=p.member_id WHERE m.id IS NULL;
SELECT 'recharge_record->member', COUNT(*) FROM recharge_record r LEFT JOIN member m ON m.id=r.member_id WHERE m.id IS NULL;
SELECT 'employee_role->role', COUNT(*) FROM employee_role er LEFT JOIN role r ON r.id=er.role_id WHERE r.id IS NULL;
SELECT 'urgency_record->orders', COUNT(*) FROM urgency_record u LEFT JOIN orders o ON o.id=u.order_id WHERE o.id IS NULL;
SELECT 'cs_message->cs_session', COUNT(*) FROM cs_message m LEFT JOIN cs_session s ON s.id=m.session_id WHERE s.id IS NULL;
SELECT 'withdrawal_record->application', COUNT(*) FROM withdrawal_record w LEFT JOIN withdrawal_application a ON a.id=w.WITHDRAWAL_ID WHERE a.id IS NULL;
SELECT 'region L3->parent', COUNT(*) FROM region r LEFT JOIN region p ON p.id=r.parent_id WHERE r.level=3 AND p.id IS NULL;
SELECT 'region L2->parent', COUNT(*) FROM region r LEFT JOIN region p ON p.id=r.parent_id WHERE r.level=2 AND p.id IS NULL;
