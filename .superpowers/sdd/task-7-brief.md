## Task 7: 会员营销页面

### member-list.html

- 表格列：name, phone, levelName, points, balance(￥), totalConsumption(￥), createdTime
- 搜索：name, phone, levelId(下拉加载 levelPage)
- 操作：查看详情(弹框)、充值(弹框: amount + giftAmount, 调用 memberRecharge)
- API 引用：`../../api/member-center.js`（memberPage, getMember, memberRecharge, memberDeductBalance, levelPage）

### level-list.html

- 表格列：name, requiredPoints, discountRate(显示为百分比), sort
- 弹框：name, requiredPoints, discountRate, sort
- API 引用：`../../api/member-center.js`（levelPage, addLevel, updateLevel, deleteLevel）

### coupon-list.html

- 表格列：name, type(满减/折扣/新客标签), conditionAmount, discountAmount, totalCount, remainCount, validDays, status
- 搜索：name, type(下拉), status
- 弹框：name, type(下拉), conditionAmount, discountAmount, totalCount, validDays, status
- API 引用：`../../api/member-center.js`（couponTemplatePage, addCouponTemplate, updateCouponTemplate, deleteCouponTemplate）

### points-list.html

- 只读列表
- 搜索：phone(会员手机号)
- 表格列：memberName, phone, type(获取/消费/过期标签), points, balance, createdTime
- API 引用：`../../api/member-center.js`（pointsPage）

### recharge-list.html

- 只读列表
- 搜索：phone(会员手机号)
- 表格列：memberName, phone, amount(￥), giftAmount(￥), createdTime
- API 引用：`../../api/member-center.js`（rechargePage）

### Steps

- [ ] **Step 1: 创建 `member-center/member-list.html`**
- [ ] **Step 2: 创建 `member-center/level-list.html`**
- [ ] **Step 3: 创建 `member-center/coupon-list.html`**
- [ ] **Step 4: 创建 `member-center/points-list.html`**
- [ ] **Step 5: 创建 `member-center/recharge-list.html`**
- [ ] **Step 6: 提交**

```bash
git add src/main/resources/backend/page/member-center/
git commit -m "feat(frontend): add member marketing pages"
```
