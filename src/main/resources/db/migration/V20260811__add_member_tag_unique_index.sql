-- 为 member_tag 表添加唯一索引，防止同一会员重复添加相同业务标签
-- 支持逻辑删除：只对 is_deleted=0 的记录生效
ALTER TABLE `member_tag` 
ADD UNIQUE INDEX `uk_tenant_member_biz`(`tenant_id`, `member_id`, `biz_tag`);