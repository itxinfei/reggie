---
argument-hint: [message]
description: 一键提交所有变更并推送到 Gitee 仓库
---

# 一键提交并推送到 Gitee

将当前所有变更提交到 Gitee 仓库。支持自定义提交信息，或自动生成。

## 用法

```
/push-gitee                          # 自动生成提交信息
/push-gitee "修复登录页面样式问题"      # 自定义提交信息
```

## 执行流程

### 1. 检查状态
```bash
rtk git status
```
- 确认有可提交的变更
- 显示变更文件列表

### 2. 暂存文件
```bash
rtk git add .
```
- 添加所有变更（新增 + 修改 + 删除）
- 不包括 `.mimocode/` 目录下的临时文件

### 3. 生成提交信息

如果用户提供了 message，直接使用。否则自动分析变更生成：

| 变更类型 | 前缀 | 示例 |
|---------|------|------|
| 新增功能 | feat: | feat: 新增会员积分功能 |
| Bug修复 | fix: | fix: 修复订单金额计算错误 |
| 文档更新 | docs: | docs: 更新 README |
| 样式调整 | style: | style: 优化登录页面样式 |
| 重构 | refactor: | refactor: 重构菜品管理模块 |
| 测试 | test: | test: 添加订单单元测试 |
| 杂项 | chore: | chore: 更新依赖版本 |

### 4. 提交
```bash
rtk git commit -m "生成的信息"
```

### 5. 推送到 Gitee
```bash
rtk git push origin main
```
- 如果当前分支不是 main/main，提示用户确认
- 推送失败时显示错误信息和解决建议

### 6. 输出结果
```
✅ 已提交并推送到 Gitee
📝 提交信息：xxx
📊 变更统计：N files changed, +X -Y
🔗 仓库地址：https://gitee.com/itxinfei/reggie
```

## 注意事项

- 自动跳过 `.mimocode/` 目录下的文件
- 如果有未跟踪的敏感文件（.env, credentials.json），会警告用户
- 推送前自动检查是否有远程仓库配置
