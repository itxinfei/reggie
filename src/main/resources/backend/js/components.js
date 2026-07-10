/**
 * 统共前端组件渲染函数
 * 适用于 iframe 架构下无构建工具的场景
 *
 * 使用方式：
 *   <script src="../../js/components.js"></script>
 *   然后在 Vue 模板中调用 window.renderStatCards() / window.renderTableBar()
 */

/**
 * 生成统计卡片 HTML 字符串
 * @param {Array} cards - 卡片配置数组
 *   [{ icon, label, value, colorClass, clickable, active, onClick, flex }]
 *   - icon: emoji 或 HTML 字符串
 *   - label: 标签文字
 *   - value: 数值（字符串或数字）
 *   - colorClass: 颜色类名（primary/success/warning/danger/info）
 *   - clickable: 是否可点击筛选（boolean）
 *   - active: 是否当前激活（boolean）
 *   - onClick: Vue 事件处理方法名（string）
 *   - flex: 是否使用桌台变体（boolean，默认 false）
 * @returns {string} HTML 字符串
 */
window.renderStatCards = function (cards) {
  return cards.map(function (card) {
    var cardClass = 'stat-card'
    if (card.flex) {
      cardClass += ' stat-card--flex'
    }
    if (card.colorClass) {
      cardClass += ' ' + card.colorClass
    }
    if (card.clickable) {
      cardClass += ' clickable'
    }
    if (card.active) {
      cardClass += ' active'
    }

    var attrs = ''
    if (card.onClick) {
      attrs += ' @click="' + card.onClick + '"'
    }

    var iconHtml = card.icon || ''

    return '<div class="' + cardClass + '"' + attrs + '>' +
      '<div class="stat-icon">' + iconHtml + '</div>' +
      '<div class="stat-label">' + (card.label || '') + '</div>' +
      '<div class="stat-value">' + (card.value != null ? card.value : 0) + '</div>' +
      '</div>'
  }).join('')
}

/**
 * 生成搜索栏（tableBar）HTML 字符串
 * @param {Object} config - 配置对象
 *   - items: [{ type, model, placeholder, options, width, onEnter, onClick, onChange }]
 *     type: 'input' | 'select' | 'date'
 *     options: [{ label, value }]（仅 select 需要）
 *   - actions: [{ text, type, onClick, icon }]
 *     type: el-button type（primary/success/danger 等）
 * @returns {string} HTML 字符串
 */
window.renderTableBar = function (config) {
  var items = config.items || []
  var actions = config.actions || []

  var html = '<div class="tableBar">'

  // 渲染筛选条件
  items.forEach(function (item) {
    var width = (item.width != null ? item.width : 200) + 'px'
    var onEnter = item.onEnter || 'handleQuery'
    var onClick = item.onClick || 'handleQuery'
    var onChange = item.onChange || 'handleQuery'

    if (item.type === 'input') {
      html += '<el-input v-model="' + item.model + '" placeholder="' + (item.placeholder || '') +
        '" style="width:' + width + '" clearable @keyup.enter.native="' + onEnter + '">' +
        '<i slot="prefix" class="el-input__icon el-icon-search" style="cursor:pointer" @click="' + onClick + '"></i>' +
        '</el-input>'
    } else if (item.type === 'select') {
      html += '<el-select v-model="' + item.model + '" placeholder="' + (item.placeholder || '') +
        '" style="width:' + width + ';margin-left:10px;" clearable @change="' + onChange + '">'
      if (item.options && item.options.length) {
        item.options.forEach(function (opt) {
          html += '<el-option label="' + (opt.label || '') + '" value="' + (opt.value != null ? opt.value : '') + '"></el-option>'
        })
      }
      html += '</el-select>'
    } else if (item.type === 'date') {
      html += '<el-date-picker v-model="' + item.model + '" clearable value-format="yyyy-MM-dd HH:mm:ss"' +
        ' type="datetimerange" placeholder="选择日期" range-separator="至"' +
        ' start-placeholder="开始日期" end-placeholder="结束日期"' +
        ' :default-time="[\'00:00:00\', \'23:59:59\']"' +
        ' style="width:' + width + ';margin-left:10px;" @change="' + onChange + '">' +
        '</el-date-picker>'
    }
  })

  // 渲染操作按钮组
  if (actions.length > 0) {
    html += '<div class="tableLab">'
    actions.forEach(function (action) {
      var iconAttr = action.icon ? ' icon="' + action.icon + '"' : ''
      html += '<el-button type="' + (action.type || 'primary') + '" size="small" @click="' + action.onClick + '"' + iconAttr + '>' + (action.text || '') + '</el-button>'
    })
    html += '</div>'
  }

  html += '</div>'
  return html
}
