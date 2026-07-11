/**
 * 后台管理系统通用组件库
 * 适用于 iframe 架构 + Vue 2 + Element UI 的无构建工具场景
 *
 * 组件列表：
 *   <stat-cards>    — 数据统计卡片组
 *   <table-bar>     — 搜索筛选 + 操作按钮栏
 *   <crud-table>    — 完整 CRUD 数据表格（含分页、多选、行操作、弹窗逻辑）
 *
 * 使用方式：
 *   1. 在 <head> 引入样式：
 *      <link rel="stylesheet" href="../../styles/components-stats-card.css" />
 *      <link rel="stylesheet" href="../../styles/components.css" />
 *   2. 在 <script> 引入 Vue/Element/Axios 后引入：
 *      <script src="../../js/components.js"></script>
 *   3. 直接在 Vue 模板中使用：
 *      <stat-cards :cards="statsConf" @card-click="onCard"/>
 *      <table-bar :search-items="searchConf" :actions="actionConf" @search="onSearch"/>
 *      <crud-table :data="tableData" :columns="columns" :page="page" .../>
 *
 * 兼容性：
 *   - 保留 window.renderStatCards() / window.renderTableBar() 旧版函数
 *   - 所有新组件通过 Vue.component() 注册，不影响已有页面
 *
 * @author Reggie Team
 * @since 2025-07-11
 */

// ============================================================
// 组件1：stat-cards — 数据统计卡片组
// ============================================================
Vue.component('stat-cards', {
  props: {
    /**
     * 卡片配置数组
     * 每项：{ key, icon, label, value, color, unit, clickable, active, flex }
     *   - key:       唯一标识（用于 active 状态匹配和 @card-click 回调）
     *   - icon:      图标（emoji 或 HTML 字符串，如 '🍽️'）
     *   - label:     标签文字（如 '菜品总数'）
     *   - value:     数值（字符串或数字）
     *   - color:     主题色 primary/success/warning/danger/info
     *                 或别名 blue/green/orange/purple
     *   - unit:      数值单位（可选，如 '元'、'人'）
     *   - clickable: 是否可点击筛选（boolean，默认 false）
     *   - active:    是否当前激活（boolean，用于筛选态高亮）
     *   - flex:      是否使用桌台变体（boolean，默认 false，图标左对齐flex布局）
     */
    cards: {
      type: Array,
      default: function () { return [] }
    },
    /** 当前激活的卡片 key（双向绑定，支持 .sync） */
    activeKey: {
      type: String,
      default: ''
    }
  },
  template:
    '<div class="stats-cards">' +
      '<div v-for="card in cards" :key="card.key || card.label"' +
        ' :class="cardClasses(card)"' +
        ' @click="onCardClick(card)">' +
        // flex 模式：图标 + 信息左右布局（使用 .stat-icon 与 components-stats-card.css 一致）
        '<template v-if="card.flex">' +
          '<div class="stat-icon"><span v-html="card.icon"></span></div>' +
          '<div class="stat-info">' +
            '<div class="stat-label">{{ card.label }}</div>' +
            '<div class="stat-value">' +
              '{{ card.value != null ? card.value : 0 }}' +
              '<small v-if="card.unit" style="font-size:14px;font-weight:400;margin-left:2px;">{{ card.unit }}</small>' +
            '</div>' +
          '</div>' +
        '</template>' +
        // 普通模式：图标 + 标签 + 数值纵向布局
        '<template v-else>' +
          '<div class="stat-icon"><span v-html="card.icon"></span></div>' +
          '<div class="stat-label">{{ card.label }}</div>' +
          '<div class="stat-value">' +
            '{{ card.value != null ? card.value : 0 }}' +
            '<small v-if="card.unit" style="font-size:14px;font-weight:400;margin-left:2px;">{{ card.unit }}</small>' +
          '</div>' +
          '<div v-if="card.sub" class="sub">' +
            '<span v-if="card.subText">{{ card.subText }}</span>' +
          '</div>' +
        '</template>' +
      '</div>' +
    '</div>',
  methods: {
    cardClasses: function (card) {
      return {
        'stat-card': true,
        'stat-card--flex': !!card.flex,
        'clickable': !!card.clickable,
        'active': !!(card.clickable && (card.active || this.activeKey === card.key)),
        'primary': card.color === 'primary' || card.color === 'blue',
        'success': card.color === 'success' || card.color === 'green',
        'warning': card.color === 'warning' || card.color === 'orange',
        'danger': card.color === 'danger',
        'info': card.color === 'info',
        'purple': card.color === 'purple'
      }
    },
    onCardClick: function (card) {
      if (card.clickable) {
        var newKey = this.activeKey === card.key ? '' : card.key
        this.$emit('update:activeKey', newKey)
        this.$emit('card-click', { key: card.key, card: card, activeKey: newKey })
      }
    }
  }
})


// ============================================================
// 组件2：table-bar — 搜索筛选 + 操作按钮栏
// ============================================================
Vue.component('table-bar', {
  props: {
    /**
     * 搜索项配置数组
     * 每项：{ type, field, placeholder, options, width, clearable, hideSearchIcon }
     *   - type:        'input' | 'select' | 'date' | 'daterange'
     *   - field:       绑定字段名（内部 v-model 管理，父组件通过 @search 接收）
     *   - placeholder: 占位提示
     *   - options:     下拉选项 [{ label, value }]（仅 select 类型）
     *   - width:       宽度，如 '200px' 或 200（默认 200）
     *   - clearable:   可否清除（默认 true）
     *   - hideSearchIcon: 是否隐藏搜索图标（默认 false）
     */
    searchItems: {
      type: Array,
      default: function () { return [] }
    },
    /**
     * 操作按钮配置数组
     * 每项：{ key, text, type, icon, size, disabled, visible }
     *   - key:     唯一标识（@action 回调参数）
     *   - text:    按钮文字
     *   - type:    el-button type（primary/success/warning/danger，默认 primary）
     *   - icon:    el-button icon（如 'el-icon-download'）
     *   - size:    尺寸（默认 'small'）
     *   - disabled: 是否禁用
     *   - visible: 是否可见（默认 true，可用于条件隐藏）
     */
    actions: {
      type: Array,
      default: function () { return [] }
    },
    /** 查询按钮 loading 状态 */
    loading: {
      type: Boolean,
      default: false
    },
    /** 是否显示查询/重置按钮（默认 true） */
    showSearchBtns: {
      type: Boolean,
      default: true
    },
    /** 查询按钮文字（默认 '查询'） */
    searchText: {
      type: String,
      default: '查询'
    },
    /** 重置按钮文字（默认 '重置'） */
    resetText: {
      type: String,
      default: '重置'
    }
  },
  data: function () {
    var values = {}
    this.searchItems.forEach(function (item) {
      values[item.field] = item.type === 'date' || item.type === 'daterange' ? '' : ''
    })
    return { searchValues: values }
  },
  template:
    '<div class="tableBar">' +
      // 搜索区域：所有搜索控件 + 查询/重置按钮
      '<div class="search-area">' +
        '<template v-for="item in searchItems">' +
          // 文本输入框
          '<el-input v-if="item.type === \'input\'" :key="\'search-\' + item.field"' +
          '  v-model="searchValues[item.field]"' +
          '  :placeholder="item.placeholder || \'请输入\'"' +
          '  :class="item.cssClass || \'\'"' +
          '  :style="item.width ? { width: toWidth(item.width) } : {}"' +
          '  :clearable="item.clearable !== false"' +
          '  @keyup.enter.native="$emit(\'search\', getSearchParams())"' +
          '>' +
          '  <i v-if="!item.hideSearchIcon" slot="prefix" class="el-input__icon el-icon-search" style="cursor:pointer" @click="$emit(\'search\', getSearchParams())"></i>' +
          '</el-input>' +
          // 下拉选择框
          '<el-select v-else-if="item.type === \'select\'" :key="\'search-\' + item.field"' +
          '  v-model="searchValues[item.field]"' +
          '  :placeholder="item.placeholder || \'请选择\'"' +
          '  :class="item.cssClass || \'\'"' +
          '  :style="item.width ? { width: toWidth(item.width) } : {}"' +
          '  :clearable="item.clearable !== false"' +
          '  @change="$emit(\'search\', getSearchParams())"' +
          '>' +
          '  <el-option v-for="opt in (item.options || [])" :key="opt.value" :label="opt.label" :value="opt.value"></el-option>' +
          '</el-select>' +
          // 日期范围选择器
          '<el-date-picker v-else-if="item.type === \'date\' || item.type === \'daterange\'" :key="\'search-\' + item.field"' +
          '  v-model="searchValues[item.field]"' +
          '  type="datetimerange"' +
          '  value-format="yyyy-MM-dd HH:mm:ss"' +
          '  :placeholder="item.placeholder || \'选择日期\'"' +
          '  range-separator="至"' +
          '  start-placeholder="开始日期"' +
          '  end-placeholder="结束日期"' +
          '  :default-time="[\'00:00:00\', \'23:59:59\']"' +
          '  :class="item.cssClass || \'\'"' +
          '  :style="item.width ? { width: toWidth(item.width) } : {}"' +
          '  :clearable="item.clearable !== false"' +
          '  @change="$emit(\'search\', getSearchParams())"' +
          '>' +
          '</el-date-picker>' +
        '</template>' +
        // 搜索扩展插槽
        '<slot name="search-extra"></slot>' +
        // 查询/重置按钮
        '<div class="search-actions">' +
          '<el-button v-if="showSearchBtns" type="primary" size="small" class="btn-query" @click="$emit(\'search\', getSearchParams())" :loading="loading">' +
          '  <i class="el-icon-search"></i> {{ searchText }}' +
          '</el-button>' +
          '<el-button v-if="showSearchBtns" size="small" plain class="btn-reset" @click="doReset">' +
          '  <i class="el-icon-refresh"></i> {{ resetText }}' +
          '</el-button>' +
        '</div>' +
      '</div>' +
      // 自定义内容插槽（如附加控件、视图切换）
      '<slot></slot>' +
      // 操作按钮组
      '<div v-if="visibleActions.length > 0" class="action-group">' +
        '<el-button v-for="btn in visibleActions" :key="btn.key"' +
        '  :type="btn.type || \'default\'"' +
        '  :size="btn.size || \'small\'"' +
        '  :icon="btn.icon || \'\'"' +
        '  :class="[\'btn-action\', getBtnClass(btn), btn.cssClass || \'\']"' +
        '  :disabled="!!btn.disabled"' +
        '  @click="$emit(\'action\', { key: btn.key, btn: btn, searchParams: getSearchParams() })"' +
        '>' +
        '  {{ btn.text }}' +
        '</el-button>' +
      '</div>' +
    '</div>',
  computed: {
    visibleActions: function () {
      return this.actions.filter(function (btn) { return btn.visible !== false })
    }
  },
  methods: {
    /** 将传入的 width 转为带 px 的 CSS 值 */
    toWidth: function (w, defaultW) {
      if (w == null) return defaultW
      var str = String(w)
      return str.indexOf('px') !== -1 || str.indexOf('%') !== -1 ? str : str + 'px'
    },
    /** 构造搜索参数对象（过滤空值） */
    getSearchParams: function () {
      var params = {}
      var self = this
      this.searchItems.forEach(function (item) {
        var val = self.searchValues[item.field]
        if (val !== '' && val !== null && val !== undefined) {
          // 日期范围特殊处理：拆分为 beginTime/endTime
          if ((item.type === 'date' || item.type === 'daterange') && Array.isArray(val) && val.length === 2) {
            var beginField = item.beginField || (item.field + 'Begin')
            var endField = item.endField || (item.field + 'End')
            params[beginField] = val[0]
            params[endField] = val[1]
          } else if ((item.type === 'date' || item.type === 'daterange') && Array.isArray(val) && val.length === 0) {
            // 日期清空：不传
          } else {
            params[item.field] = val
          }
        }
      })
      return params
    },
    /** 重置所有搜索条件 */
    doReset: function () {
      var self = this
      this.searchItems.forEach(function (item) {
        self.$set(self.searchValues, item.field, item.type === 'date' || item.type === 'daterange' ? [] : '')
      })
      this.$emit('reset', {})
    },
    /** 外部可调用的清除方法：this.$refs.xxx.clearSearch() */
    clearSearch: function () {
      this.doReset()
    },
    /** 根据按钮 key 自动映射功能 CSS class */
    getBtnClass: function (btn) {
      var key = (btn.key || '').toLowerCase()
      if (key.indexOf('add') !== -1 || key.indexOf('create') !== -1 || key.indexOf('new') !== -1) return 'btn-add'
      if (key.indexOf('delete') !== -1 || key.indexOf('remove') !== -1 || key.indexOf('batchdelete') !== -1) return 'btn-delete'
      if (key.indexOf('export') !== -1) return 'btn-export'
      if (key.indexOf('refresh') !== -1) return 'btn-refresh'
      if (key.indexOf('warning') !== -1) return 'btn-warning-status'
      return ''
    }
  }
})


// ============================================================
// 组件3：crud-table — 完整 CRUD 数据表格
// ============================================================
Vue.component('crud-table', {
  props: {
    /** 表格数据源 */
    data: {
      type: Array,
      default: function () { return [] }
    },
    /**
     * 列配置数组
     * 每项：{ prop, label, width, minWidth, align, fixed, sortable, slot, formatter, showOverflowTooltip }
     *   - prop:     字段名
     *   - label:    列头文字
     *   - width:    列宽（如 180 或 '180px'）
     *   - minWidth: 最小列宽
     *   - align:    对齐方式（默认 'left'）
     *   - fixed:    固定列 'left' | 'right'
     *   - sortable: 是否可排序
     *   - slot:     是否使用插槽渲染（true 时通过 #col-{prop} 自定义）
     *   - formatter: 格式化函数 (value, row, col) => string
     *   - showOverflowTooltip: 溢出省略提示
     */
    columns: {
      type: Array,
      default: function () { return [] }
    },
    /** 是否显示多选框列 */
    selection: {
      type: Boolean,
      default: false
    },
    /** 多选框列宽 */
    selectionWidth: {
      type: [Number, String],
      default: 50
    },
    /** 是否显示行号列（序号从 pageStart 计算） */
    showIndex: {
      type: Boolean,
      default: false
    },
    /** 行号列标签 */
    indexLabel: {
      type: String,
      default: '序号'
    },
    /** 行号列宽 */
    indexWidth: {
      type: [Number, String],
      default: 60
    },
    /** 是否显示操作列 */
    showActions: {
      type: Boolean,
      default: false
    },
    /** 操作列标签 */
    actionsLabel: {
      type: String,
      default: '操作'
    },
    /** 操作列宽 */
    actionsWidth: {
      type: [Number, String],
      default: 180
    },
    /** 操作列对齐 */
    actionsAlign: {
      type: String,
      default: 'center'
    },
    /** 表格加载状态 */
    loading: {
      type: Boolean,
      default: false
    },
    /** 表格加 stripe 斑马纹（默认 true） */
    stripe: {
      type: Boolean,
      default: true
    },
    /** 表格加 border 边框（默认 true） */
    border: {
      type: Boolean,
      default: true
    },
    /** 表格高度 */
    maxHeight: {
      type: [Number, String],
      default: undefined
    },
    /** 表格尺寸 */
    size: {
      type: String,
      default: ''
    },
    /** 当前页码 */
    page: {
      type: Number,
      default: 1
    },
    /** 每页条数 */
    pageSize: {
      type: Number,
      default: 10
    },
    /** 总条数 */
    total: {
      type: Number,
      default: 0
    },
    /** 是否显示分页 */
    showPagination: {
      type: Boolean,
      default: true
    },
    /** 分页每页条数选项 */
    pageSizes: {
      type: Array,
      default: function () { return [10, 20, 30, 40] }
    },
    /** 默认文案（数据为空时显示） */
    emptyText: {
      type: String,
      default: '暂无数据'
    }
  },
  template:
    '<div class="crud-table-wrapper">' +
      // ===== 表格 =====
      '<el-table' +
      '  ref="elTable"' +
      '  :data="data"' +
      '  :stripe="stripe"' +
      '  :border="border"' +
      '  :size="size"' +
      '  :max-height="maxHeight"' +
      '  :default-sort="{prop: \'updateTime\', order: \'descending\'}"' +
      '  v-loading="loading"' +
      '  class="tableBox"' +
      '  @selection-change="onSelectionChange"' +
      '  @sort-change="onSortChange"' +
      '>' +
      // 多选列
      '<el-table-column v-if="selection" type="selection" align="center" :width="selectionWidth"></el-table-column>' +
      // 行号列
      '<el-table-column v-if="showIndex" type="index" :label="indexLabel" :width="indexWidth" align="center"></el-table-column>' +
      // 数据列
      '<el-table-column' +
      '  v-for="col in columns"' +
      '  :key="col.prop"' +
      '  :prop="col.prop"' +
      '  :label="col.label"' +
      '  :width="col.width"' +
      '  :min-width="col.minWidth"' +
      '  :align="col.align || \'left\'"' +
      '  :fixed="col.fixed"' +
      '  :sortable="col.sortable ? \'custom\' : false"' +
      '  :show-overflow-tooltip="!!col.showOverflowTooltip"' +
      '>' +
        // 修改点：合并为单一 template，避免 v-if/v-else 多片段在 el-table-column 中渲染异常
        '<template slot-scope="scope">' +
          '<slot v-if="col.slot" :name="\'col-\' + col.prop" :row="scope.row" :col="col" :$index="scope.$index">' +
            '<span v-if="col.formatter">{{ col.formatter(scope.row[col.prop], scope.row, col) }}</span>' +
            '<span v-else>{{ scope.row[col.prop] }}</span>' +
          '</slot>' +
          '<span v-else-if="col.formatter">{{ col.formatter(scope.row[col.prop], scope.row, col) }}</span>' +
          '<span v-else>{{ scope.row[col.prop] }}</span>' +
        '</template>' +
      '</el-table-column>' +
      // 操作列
      '<el-table-column v-if="showActions" :label="actionsLabel" :width="actionsWidth" :align="actionsAlign" fixed="right">' +
        '<template slot-scope="scope">' +
          '<slot name="actions" :row="scope.row" :$index="scope.$index" :size="size"></slot>' +
        '</template>' +
      '</el-table-column>' +
      // 空状态提示
      '<template slot="empty">' +
        '<div style="padding:40px 0;">' +
          '<i class="el-icon-document" style="font-size:48px;color:#c0c4cc;"></i>' +
          '<p style="margin-top:8px;color:#c0c4cc;font-size:14px;">{{ emptyText }}</p>' +
        '</div>' +
      '</template>' +
    '</el-table>' +
    // ===== 分页 =====
    '<el-pagination v-if="showPagination"' +
    '  class="pageList"' +
    '  :page-sizes="pageSizes"' +
    '  :page-size="pageSize"' +
    '  :current-page.sync="currentPage"' +
    '  layout="total, sizes, prev, pager, next, jumper"' +
    '  :total="total"' +
    '  @size-change="onSizeChange"' +
    '  @current-change="onPageChange"' +
    '></el-pagination>' +
  '</div>',
  data: function () {
    return {
      currentPage: this.page,
      selectedRows: []
    }
  },
  watch: {
    page: function (val) {
      this.currentPage = val
    }
  },
  methods: {
    /** 获取已选中的行数据 */
    getSelectedRows: function () {
      return this.selectedRows
    },
    /** 清除选中状态 */
    clearSelection: function () {
      if (this.$refs.elTable) {
        this.$refs.elTable.clearSelection()
      }
      this.selectedRows = []
    },
    // ---- 内部事件处理 ----
    onSelectionChange: function (val) {
      this.selectedRows = val
      var ids = val.map(function (row) { return row.id })
      this.$emit('selection-change', { rows: val, ids: ids, count: val.length })
    },
    onSortChange: function (sortInfo) {
      this.$emit('sort-change', sortInfo)
    },
    onSizeChange: function (val) {
      this.currentPage = 1
      this.$emit('size-change', val)
      this.$emit('page-change', { page: 1, pageSize: val })
    },
    onPageChange: function (val) {
      this.$emit('page-change', { page: val, pageSize: this.pageSize })
    }
  }
})


// ============================================================
// 组件4：crud-dialog — 新增/编辑弹窗（可选，提供标准 CRUD 弹窗壳）
// ============================================================
Vue.component('crud-dialog', {
  props: {
    /** 弹窗标题 */
    title: {
      type: String,
      default: '新增'
    },
    /** 是否可见（支持 .sync） */
    visible: {
      type: Boolean,
      default: false
    },
    /** 弹窗宽度 */
    width: {
      type: String,
      default: '600px'
    },
    /** 是否点击遮罩层关闭 */
    closeOnClickModal: {
      type: Boolean,
      default: false
    },
    /** 确认按钮 loading */
    submitLoading: {
      type: Boolean,
      default: false
    },
    /** 确认按钮文字 */
    submitText: {
      type: String,
      default: '确 定'
    },
    /** 取消按钮文字 */
    cancelText: {
      type: String,
      default: '取 消'
    },
    /** 是否显示确认按钮 */
    showSubmit: {
      type: Boolean,
      default: true
    },
    /** 是否显示取消按钮 */
    showCancel: {
      type: Boolean,
      default: true
    }
  },
  data: function () {
    return { dialogVisible: this.visible }
  },
  watch: {
    visible: function (val) {
      this.dialogVisible = val
    }
  },
  template:
    '<el-dialog' +
    '  :title="title"' +
    '  :visible.sync="dialogVisible"' +
    '  :width="width"' +
    '  :close-on-click-modal="closeOnClickModal"' +
    '  :before-close="handleClose"' +
    '>' +
      // 主体内容插槽
      '<slot></slot>' +
      // 底部按钮区
      '<span slot="footer" class="dialog-footer">' +
        '<slot name="footer">' +
          '<el-button v-if="showCancel" size="medium" @click="handleClose">{{ cancelText }}</el-button>' +
          '<el-button v-if="showSubmit" type="primary" size="medium" :loading="submitLoading" @click="$emit(\'submit\')">{{ submitText }}</el-button>' +
        '</slot>' +
      '</span>' +
    '</el-dialog>',
  methods: {
    handleClose: function () {
      this.$emit('update:visible', false)
      this.$emit('close')
    },
    /** 外部调用打开弹窗 */
    open: function () {
      this.$emit('update:visible', true)
    },
    /** 外部调用关闭弹窗 */
    close: function () {
      this.handleClose()
    }
  }
})


// ============================================================
// 兼容旧版：保留 window 上的渲染函数（已有页面无需修改）
// ============================================================
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

    return '<div class="' + cardClass + '"' + attrs + '>' +
      '<div class="stat-icon">' + (card.icon || '') + '</div>' +
      '<div class="stat-label">' + (card.label || '') + '</div>' +
      '<div class="stat-value">' + (card.value != null ? card.value : 0) + '</div>' +
      '</div>'
  }).join('')
}

window.renderTableBar = function (config) {
  var items = config.items || []
  var actions = config.actions || []

  var html = '<div class="tableBar">'

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
