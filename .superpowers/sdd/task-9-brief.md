## Task 9: 菜单入口

修改 `backend/index.html`，在 `menuList` 数组末尾追加 7 个菜单项：

```javascript
{ id: '7', name: '打印管理', url: 'page/printer/config-list.html', icon: 'icon-printer' },
{ id: '8', name: '支付管理', url: 'page/payment/order-list.html', icon: 'icon-payment' },
{ id: '9', name: '外卖平台', url: 'page/delivery/order-list.html', icon: 'icon-delivery' },
{ id: '10', name: '堂食管理', url: 'page/dining/table-list.html', icon: 'icon-dining' },
{ id: '11', name: '进销存管理', url: 'page/inventory/material-list.html', icon: 'icon-inventory' },
{ id: '12', name: '会员营销', url: 'page/member-center/member-list.html', icon: 'icon-member-center' },
{ id: '13', name: '经营报表', url: 'page/report/daily.html', icon: 'icon-report' },
```

注意：菜单 ID 从 7 开始（已有 2-6 对应员工/分类/菜品/套餐/订单，ID 1 预留）。

### Steps

- [ ] **Step 1: 修改 `backend/index.html`，追加菜单项**
- [ ] **Step 2: 提交**

```bash
git add src/main/resources/backend/index.html
git commit -m "feat(frontend): add menu entries for 7 business modules"
```
