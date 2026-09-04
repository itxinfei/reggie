-- 会员积分发放幂等兜底：同一租户下同一业务（biz_type+biz_id）只允许一条 IN 流水
-- 说明：本地手动执行（项目约定 db/migration 仅作参考脚本，不自动迁移）
-- 作用：防止并发场景（现金支付线程 + 订单完成异步监听）重复发放积分

ALTER TABLE `points_record`
    ADD UNIQUE KEY `uq_points_biz` (`tenant_id`, `biz_type`, `biz_id`, `type`);
