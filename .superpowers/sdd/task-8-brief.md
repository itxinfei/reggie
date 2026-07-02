## Task 8: 经营报表页面（含 ECharts）

所有 4 个报表页面引入 ECharts CDN：
```html
<script src="https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js"></script>
```

在 `mounted()` 中 init 图表，`beforeDestroy()` 中 dispose：

```javascript
mounted() {
  this.$nextTick(() => {
    this.chart = echarts.init(document.getElementById('chart'))
    window.addEventListener('resize', () => this.chart.resize())
    this.loadChart()
  })
},
beforeDestroy() {
  window.removeEventListener('resize', () => this.chart.resize())
  if (this.chart) this.chart.dispose()
}
```

### daily.html

- 日期选择：`el-date-picker` type="date"，默认当天
- 指标卡片：4 个 `el-card` 行内显示营业额、订单数、客单价、翻台率
- 趋势图：ECharts 折线图，X 轴日期，Y 轴金额，显示近 7 天趋势
- API 引用：`../../api/report.js`（reportDaily）

图表 option 示例：
```javascript
const option = {
  xAxis: { type: 'category', data: dates },
  yAxis: { type: 'value' },
  series: [{ type: 'line', data: amounts, smooth: true, areaStyle: {} }],
  tooltip: { trigger: 'axis' }
}
```

### dish-ranking.html

- 日期范围：`el-date-picker` type="daterange"
- 柱状图：ECharts，X 轴菜品名，Y 轴销量，Top 10
- 排行表格：rank, name, salesCount, amount(￥)
- 导出按钮：调用 reportExport 下载 CSV
- API 引用：`../../api/report.js`（reportDishRanking, reportExport）

### time-slot.html

- 日期选择：`el-date-picker` type="date"
- 饼图：ECharts，显示早/中/晚/夜各时段订单占比
- 数据表格：timeSlot, orderCount, percentage, amount

### payment-analysis.html

- 日期范围：`el-date-picker` type="daterange"
- 饼图：ECharts，显示支付宝/微信/其他占比
- 数据表格：channel, transactionCount, amount, percentage

### Steps

- [ ] **Step 1: 创建 `report/daily.html`**
- [ ] **Step 2: 创建 `report/dish-ranking.html`**
- [ ] **Step 3: 创建 `report/time-slot.html`**
- [ ] **Step 4: 创建 `report/payment-analysis.html`**
- [ ] **Step 5: 提交**

```bash
git add src/main/resources/backend/page/report/
git commit -m "feat(frontend): add business report pages with ECharts"
```
