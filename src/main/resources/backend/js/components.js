// 三端共享 imgPath（单一真源，幂等注入）：必须先于本文件所有 Vue 组件定义
document.write('<script src="/shared/js/img-path.js?v=20260924"><\/script>');
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
     *   - colorClass: 自定义 CSS 类名（如 'pending'/'cooking'/'alarm'，追加到卡片根元素，用于大屏等特殊场景）
     */
    cards: {
      type: Array,
      default: function () { return [] }
    },
    /** 当前激活的卡片 key（双向绑定，支持 .sync） */
    activeKey: {
      type: String,
      default: ''
    },
    /** 数据加载状态（开启后卡片区显示 loading 遮罩） */
    loading: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    /**
     * 根据卡片数量自动选择列数布局：
     * 1~3 张时避免仍用 4 列网格导致卡片过窄、数值与图标重叠；
     * 4 张及以上保持默认 4 列。
     */
    rootClass: function () {
      var n = Math.min(this.cards.length, 4)
      if (n <= 1) return 'stats-cards stats-cards--col-1'
      if (n === 2) return 'stats-cards stats-cards--col-2'
      if (n === 3) return 'stats-cards stats-cards--col-3'
      return 'stats-cards'
    }
  },
  template:
    '<div :class="rootClass">' +
      '<div v-if="loading" class="stats-cards__loading"><i class="ri-loader-4-line"></i></div>' +
      '<div v-for="card in cards" :key="card.key || card.label"' +
        ' :class="cardClasses(card)"' +
        ' :tabindex="card.clickable ? 0 : null"' +
        ' :role="card.clickable ? \'button\' : null"' +
        ' :aria-pressed="card.clickable ? isActive(card) : null"' +
        ' :aria-label="card.clickable ? (card.label + \'：\' + (card.value != null ? card.value : 0) + (card.unit || \'\') + \'，点击切换筛选\') : null"' +
        ' @click="onCardClick(card)"' +
        ' @keydown.enter.prevent="onCardClick(card)"' +
        ' @keydown.space.prevent="onCardClick(card)">' +
        // flex 模式：图标 + 信息左右布局（使用 .stat-icon 与 components-stats-card.css 一致）
        '<template v-if="card.flex">' +
          '<div class="stat-icon">' +
            '<i v-if="isIconClass(card.icon)" :class="card.icon" aria-hidden="true"></i>' +
            '<span v-else v-text="card.icon"></span>' +
          '</div>' +
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
          '<div class="stat-icon">' +
            '<i v-if="isIconClass(card.icon)" :class="card.icon" aria-hidden="true"></i>' +
            '<span v-else v-text="card.icon"></span>' +
          '</div>' +
          '<div class="stat-label">{{ card.label }}</div>' +
          // error 态：统计接口失败（如数据库未连接），提示点击重试，而非静默显示 0
          '<div v-if="card.error" class="stat-value stat-value--error" aria-label="统计加载失败，点击重试">' +
            '<i class="ri-error-warning-line"></i> 加载失败' +
            '<div style="font-size:12px;font-weight:400;">点击重试</div>' +
          '</div>' +
          '<div v-else class="stat-value">' +
            '{{ card.value != null ? card.value : 0 }}' +
            '<small v-if="card.unit" style="font-size:14px;font-weight:400;margin-left:2px;">{{ card.unit }}</small>' +
          '</div>' +
          '<div v-if="card.subText" class="sub">' +
            '<span>{{ card.subText }}</span>' +
          '</div>' +
        '</template>' +
        // 卡片追加插槽：允许大屏等特殊场景在卡片内追加自定义内容（如进度条、实时指标）
        '<slot name="card-append" :card="card"></slot>' +
      '</div>' +
    '</div>',
  methods: {
    /**
     * 判断 icon 是否为 RemixIcon class（以 ri- 开头）
     * 是则使用 <i> 标签渲染，否则作为 emoji 文本渲染
     */
    isIconClass: function(icon) {
      return typeof icon === 'string' && /^(ri-|el-icon-)/.test(icon);
    },
    cardClasses: function (card) {
      var cls = {
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
      // colorClass：追加自定义 CSS 类名（如大屏场景的 pending/cooking/alarm 等）
      if (card.colorClass) {
        var parts = String(card.colorClass).split(/\s+/)
        parts.forEach(function (c) { if (c) cls[c] = true })
      }
      return cls
    },
    isActive: function (card) {
      return !!(card.clickable && (card.active || this.activeKey === card.key))
    },
    onCardClick: function (card) {
      // 加载失败态：点击触发重试，不参与筛选切换
      if (card.error) {
        this.$emit('retry', { key: card.key, card: card })
        return
      }
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
    var items = this.searchItems || []
    items.forEach(function (item) {
      values[item.field] = ''
    })
    return {
      searchValues: values,
      // 修改点：日期时间范围选择器的快捷选项，大幅提升筛选效率
      pickerOptions: {
        shortcuts: [{
          text: '今天',
          onClick: function (picker) {
            var start = new Date(); start.setHours(0, 0, 0, 0)
            var end = new Date(); end.setHours(23, 59, 59, 999)
            picker.$emit('pick', [start, end])
          }
        }, {
          text: '昨天',
          onClick: function (picker) {
            var start = new Date(); start.setDate(start.getDate() - 1); start.setHours(0, 0, 0, 0)
            var end = new Date(); end.setDate(end.getDate() - 1); end.setHours(23, 59, 59, 999)
            picker.$emit('pick', [start, end])
          }
        }, {
          text: '最近7天',
          onClick: function (picker) {
            var end = new Date(); end.setHours(23, 59, 59, 999)
            var start = new Date(); start.setDate(start.getDate() - 6); start.setHours(0, 0, 0, 0)
            picker.$emit('pick', [start, end])
          }
        }, {
          text: '最近30天',
          onClick: function (picker) {
            var end = new Date(); end.setHours(23, 59, 59, 999)
            var start = new Date(); start.setDate(start.getDate() - 29); start.setHours(0, 0, 0, 0)
            picker.$emit('pick', [start, end])
          }
        }, {
          text: '本月',
          onClick: function (picker) {
            var now = new Date()
            var start = new Date(now.getFullYear(), now.getMonth(), 1)
            var end = new Date(now.getFullYear(), now.getMonth() + 1, 0)
            end.setHours(23, 59, 59, 999)
            picker.$emit('pick', [start, end])
          }
        }, {
          text: '上月',
          onClick: function (picker) {
            var now = new Date()
            var start = new Date(now.getFullYear(), now.getMonth() - 1, 1)
            var end = new Date(now.getFullYear(), now.getMonth(), 0)
            end.setHours(23, 59, 59, 999)
            picker.$emit('pick', [start, end])
          }
        }]
      }
    }
  },
  template:
    '<div class="tableBar">' +
      // 搜索区域：所有搜索控件 + 查询/重置按钮
      '<div class="search-area" role="search" aria-label="搜索筛选">' +
        '<template v-for="item in searchItems">' +
          // 文本输入框
          '<el-input v-if="item.type === \'input\'" :key="\'search-\' + item.field"' +
          '  v-model="searchValues[item.field]"' +
          '  :placeholder="item.placeholder || \'请输入\'"' +
          '  :aria-label="item.label || item.placeholder || \'搜索条件\'"' +
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
          '  :aria-label="item.label || item.placeholder || \'筛选条件\'"' +
          '  :class="item.cssClass || \'\'"' +
          '  :style="item.width ? { width: toWidth(item.width) } : {}"' +
          '  :clearable="item.clearable !== false"' +
          '  @change="$emit(\'search\', getSearchParams())"' +
          '>' +
          '  <el-option v-for="opt in (item.options || [])" :key="opt.value" :label="opt.label" :value="opt.value"></el-option>' +
          '</el-select>' +
          // 日期范围选择器（修改点：添加 picker-options/unlink-panels/format/editable，优化时间选择体验）
          '<el-date-picker v-else-if="item.type === \'date\' || item.type === \'daterange\'" :key="\'search-\' + item.field"' +
          '  v-model="searchValues[item.field]"' +
          '  type="datetimerange"' +
          '  value-format="yyyy-MM-dd HH:mm:ss"' +
          '  format="yyyy-MM-dd HH:mm"' +
          '  :editable="false"' +
          '  unlink-panels' +
          '  :picker-options="pickerOptions"' +
          '  :placeholder="item.placeholder || \'选择日期范围\'"' +
          '  :aria-label="item.label || item.placeholder || \'时间范围\'"' +
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
          // 日期范围特殊处理：拆分为 beginTime/endTime（F3：仅处理长度 2，长度 0/1 均视为无效不传）
          if ((item.type === 'date' || item.type === 'daterange') && Array.isArray(val)) {
            if (val.length === 2) {
              var beginField = item.beginField || (item.field + 'Begin')
              var endField = item.endField || (item.field + 'End')
              params[beginField] = val[0]
              params[endField] = val[1]
            }
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
     * 每项：{ prop, label, width, minWidth, align, fixed, sortable, slot, formatter, showOverflowTooltip, type, className }
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
     *   - type:     单元格语义类型，'money'|'number' 时自动右对齐 + tabular-nums 等宽数字（金额列推荐 'money'）
     *   - className: 透传到该列的自定义 class-name（与 type 叠加，不冲突）
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
    /** 修改点：操作列是否固定在右侧（默认 false）。固定后可避免表格总宽不足时被压缩、导致末尾按钮被 .cell overflow 截断 */
    actionsFixed: {
      type: Boolean,
      default: false
    },
    /**
     * 是否渲染内置只读"查看"按钮（默认 false，对既有页面零影响）。
     * 开启后，操作列会自动在页面自定义按钮之前渲染一个"查看"按钮，
     * 点击打开通用只读详情弹窗（按 columns 渲染 标签:值），无需各页单独写弹窗。
     */
    viewable: {
      type: Boolean,
      default: false
    },
    /** 内置查看弹窗标题 */
    viewTitle: {
      type: String,
      default: '查看'
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
    },
    /**
     * 空状态辅助提示（显示在 emptyText 下方）。默认使用中性文案，适配全部列表页
     * （含报表/日志/纯查询等没有"新建"按钮的页面，不再出现"点击右上角新建"的误导）。
     * 确需新建引导的 CRUD 页面可显式传 empty-hint 覆盖，
     * 例如：'试试调整筛选条件，或点击右上角"新建"添加一条记录'。
     */
    emptyHint: {
      type: String,
      default: '试试调整筛选条件'
    },
    /** 表格区域无障碍标签（role=region，配合页内 aria-label 透传） */
    ariaLabel: {
      type: String,
      default: ''
    },
    /**
     * 是否开启展开行（type="expand"）。默认关闭，对既有页面零影响。
     * 开启后可通过具名插槽 #expand 自定义展开内容，并监听 expand-change 事件。
     */
    expand: {
      type: Boolean,
      default: false
    },
    /** 展开列宽度 */
    expandWidth: {
      type: Number,
      default: 48
    },
    /** 行数据的 Key（同 el-table 的 row-key），展开行/保留状态时需要 */
    rowKey: {
      type: String,
      default: ''
    },
    /** 修改点：F2 行主键字段名（默认 'id'），避免批量操作强耦合 row.id */
    rowIdKey: {
      type: String,
      default: 'id'
    },
    /** 修改点：F4 默认排序（默认 null 不排序，避免硬编码 updateTime 触发意外服务端排序请求） */
    defaultSort: {
      type: Object,
      default: null
    }
  },
  template:
    '<div class="crud-table-wrapper" role="region" :aria-label="ariaLabel || \'数据列表\'">' +
      // ===== 加载骨架屏（首次加载且无数据时显示，替代纯转圈；Element UI 2.15 无 el-skeleton，用纯 CSS 脉冲骨架） =====
      '<div v-if="showSkeleton" class="ds-skeleton-table">' +
        '<div v-for="i in 5" :key="i" class="ds-skeleton-row">' +
          '<div v-for="c in skeletonCols" :key="c" class="ds-skeleton-cell"><div class="ds-skeleton-bar"></div></div>' +
        '</div>' +
      '</div>' +
      // ===== 表格 =====
      '<el-table v-else' +
      '  ref="elTable"' +
      '  :data="data"' +
      '  :stripe="stripe"' +
      '  :border="border"' +
      '  :size="size"' +
      '  :row-key="rowKey || undefined"' +
      '  :max-height="maxHeight"' +
      '  :default-sort="defaultSort || undefined"' +
      '  highlight-current-row' +
      '  v-loading="loading"' +
      '  class="tableBox"' +
      '  @selection-change="onSelectionChange"' +
      '  @sort-change="onSortChange"' +
      '  @expand-change="onExpandChange"' +
      '  @row-click="onRowClick"' +
      '>' +
      // 展开行（可选）
      '<el-table-column v-if="expand" type="expand" :width="expandWidth">' +
        '<template slot-scope="props">' +
          '<slot name="expand" :row="props.row" :$index="props.$index">' +
            '<div style="padding:16px;color:var(--text-muted);">暂无展开内容</div>' +
          '</slot>' +
        '</template>' +
      '</el-table-column>' +
      // 多选列（页面显式传 row-key 时开启跨翻页保留勾选；未传则维持默认单页行为）
      '<el-table-column v-if="selection" type="selection" align="center" :width="selectionWidth" :reserve-selection="!!rowKey"></el-table-column>' +
      // 行号列
      '<el-table-column v-if="showIndex" type="index" :label="indexLabel" :width="indexWidth" align="center"></el-table-column>' +
      // 数据列
      // 修改点：列宽策略改为内容驱动 —— 页面配置的 width 作为 min-width（下限），
      // 表格在容器内自动按内容伸缩均分，整行完整展示，避免固定 width 总和溢出导致横向滚动。
      '<el-table-column' +
      '  v-for="col in columns"' +
      '  :key="col.prop"' +
      '  :prop="col.prop"' +
      '  :label="col.label"' +
      '  :min-width="resolveColMinWidth(col)"' +
      '  :align="resolveColAlign(col)"' +
      '  :header-align="resolveColAlign(col)"' +
      '  :class-name="(col.type === \'money\' ? \'ds-money\' : (col.type === \'number\' ? \'ds-num\' : \'\')) + (col.className ? \' \' + col.className : \'\')"' +
      '  :fixed="col.fixed"' +
      '  :sortable="col.sortable ? \'custom\' : false"' +
      // 修改点(2026-08-27)：showOverflowTooltip 默认行为从"默认开启"反转为"默认关闭"。
// - 此前 `!== false`：页面未显式设置时列内容超出 min-width 即截断为省略号 + tooltip，
//   导致用户看到的是一堆"……"，数据不完整，需悬停 tooltip 才能看到全文，违背"完整展示数据"的需求。
// - 现在 `=== true`：默认关闭 tooltip，内容超出时自然换行 + 行高自适应（配合 components.css / page.css 的 min-height:48px + height:auto），
//   超长内容（身份证、地址、备注）通过换行完整呈现；仅在 URL、错误信息等确实不适合换行的列显式 `showOverflowTooltip: true`。
'  :show-overflow-tooltip="col.showOverflowTooltip === true"' +
      '>' +
        // 修改点：合并为单一 template，避免 v-if/v-else 多片段在 el-table-column 中渲染异常；
        // money/number 列在无自定义 formatter 时自动千分位格式化，保证金额展示统一
        '<template slot-scope="scope">' +
          '<slot v-if="col.slot" :name="\'col-\' + col.prop" :row="scope.row" :col="col" :$index="scope.$index">' +
            '<span v-if="typeof col.formatter === \'function\'">{{ col.formatter(scope.row[col.prop], scope.row, col) }}</span>' +
            '<span v-else-if="col.type === \'money\'">¥{{ formatMoney(scope.row[col.prop]) }}</span>' +
            '<span v-else-if="col.type === \'number\'">{{ formatNumber(scope.row[col.prop]) }}</span>' +
            '<span v-else>{{ scope.row[col.prop] }}</span>' +
          '</slot>' +
          '<span v-else-if="typeof col.formatter === \'function\'">{{ col.formatter(scope.row[col.prop], scope.row, col) }}</span>' +
          '<span v-else-if="col.type === \'money\'">¥{{ formatMoney(scope.row[col.prop]) }}</span>' +
          '<span v-else-if="col.type === \'number\'">{{ formatNumber(scope.row[col.prop]) }}</span>' +
          '<span v-else>{{ scope.row[col.prop] }}</span>' +
        '</template>' +
      '</el-table-column>' +
      // 操作列
      // 操作列：用 min-width 而非 width —— 页面手填值作为最小宽度，
      // 按钮数量变化时列自动扩宽，避免操作按钮被截断（M1 修复）
      // 修改点：支持 actionsFixed 固定到右侧，并加 crud-actions-col class 便于 CSS nowrap 防截断
      //
      // 【操作列收敛契约 —— 所有列表页 #actions 插槽必须遵守】
      // 1. 常显主操作 ≤3 个；超出的操作收进「更多」el-dropdown（trigger="click"，@command 分发，
      //    范本见 page/dining/table-list.html、page/platform/order-list.html）。
      // 2. actions-width 宽度档：2 个元素 120 / 3 个元素 180 / 4 个元素 240（「更多」触发器算 1 个元素）。
      // 3. 语义按钮类（styles/page.css）：查看/编辑 btn-view；启用/接单/上架等肯定流转 btn-success；
      //    停用/暂停 btn-warning；删除/取消/拒单/退款等否定操作 btn-delete（dropdown 内项用内联 danger 色）。
      // 4. 按钮统一 type="text" size="small"，禁止 mini；「更多」触发器用 class="text-info" + el-icon-arrow-down。
      '<el-table-column v-if="showActions" :label="actionsLabel" :min-width="actionsWidth" :align="actionsAlign" :header-align="actionsAlign" :fixed="actionsFixed ? \'right\' : false" class-name="crud-actions-col">' +
        '<template slot-scope="scope">' +
          '<el-button v-if="viewable" type="text" size="small" class="btn-view" @click="openView(scope.row)">查看</el-button>' +
          '<slot name="actions" :row="scope.row" :$index="scope.$index" :size="size"></slot>' +
        '</template>' +
      '</el-table-column>' +
      // 空状态提示（样式收敛在 components.css 的 .ds-table-empty，禁止内联硬编码色）
      '<template slot="empty">' +
        '<div class="ds-table-empty">' +
          '<i class="el-icon-document"></i>' +
          '<p>{{ emptyText }}</p>' +
          '<p v-if="emptyHint" class="ds-table-empty__hint">{{ emptyHint }}</p>' +
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
    // ===== 内置只读查看弹窗（viewable 开启时渲染） =====
    '<el-dialog :title="viewTitle" :visible.sync="viewVisible" width="560px" :close-on-click-modal="false" append-to-body class="crud-view-dialog">' +
      '<div v-if="viewRow" class="crud-view-body">' +
        '<div v-for="col in viewColumns" :key="col.prop" class="crud-view-row">' +
          '<div class="crud-view-label">{{ col.label }}</div>' +
          '<div class="crud-view-value">{{ formatViewValue(viewRow, col) }}</div>' +
        '</div>' +
      '</div>' +
      '<div slot="footer" class="dialog-footer">' +
        '<el-button @click="viewVisible = false">关 闭</el-button>' +
      '</div>' +
    '</el-dialog>' +
  '</div>',
  data: function () {
    return {
      currentPage: this.page,
      selectedRows: [],
      viewVisible: false,
      viewRow: null
    }
  },
  watch: {
    page: function (val) {
      this.currentPage = val
    }
  },
  computed: {
    /** 骨架屏：首次加载且无数据时显示（避免空表 + 转圈的割裂感） */
    showSkeleton: function () {
      return this.loading && (!this.data || this.data.length === 0)
    },
    /** 骨架屏列数：数据列 + 可选的操作/多选/序号列 */
    skeletonCols: function () {
      var n = (this.columns || []).length
      if (this.showActions) n += 1
      if (this.selection) n += 1
      if (this.showIndex) n += 1
      return n
    },
    /** 内置查看弹窗的字段列表（排除选择/序号/展开等特殊列） */
    viewColumns: function () {
      var cols = this.columns || []
      return cols.filter(function (c) {
        return c && c.prop && c.label && c.type !== 'selection' && c.type !== 'index' && c.type !== 'expand'
      })
    }
  },
  methods: {
    /**
     * 列对齐解析：
     *  - 全站表格统一居中（修改点 2026-09-01：用户要求"表头与内容必须居中"，
     *    金额/数字列不再默认右对齐，与文本列一致居中展示）
     *  - 页面显式 align 仍可覆盖默认
     */
    resolveColAlign: function (col) {
      return col.align || 'center'
    },
    /** 列宽解析（内容驱动策略）：
     *  - 优先 minWidth（页面显式下限）
     *  - 否则 width 降级为下限（历史配置兼容）
     *  - 都未设置则返回 undefined，由 Element UI 按内容自动分配
     * 目标：整行完整展示，避免固定 width 总和溢出导致横向滚动。
     */
    resolveColMinWidth: function (col) {
      if (col.minWidth) return col.minWidth
      if (col.width) return col.width
      return undefined
    },
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
      var self = this
      var idKey = self.rowIdKey || 'id'
      this.selectedRows = val
      var ids = val.map(function (row) { return row[idKey] })
      this.$emit('selection-change', { rows: val, ids: ids, count: val.length })
    },
    onSortChange: function (sortInfo) {
      this.$emit('sort-change', sortInfo)
    },
    /** 展开行变化：透传给父页面（用于懒加载明细） */
    onExpandChange: function (row, expandedRows) {
      this.$emit('expand-change', row, expandedRows)
    },
    /** 行点击：透传给父页面（如打开详情） */
    onRowClick: function (row, column, event) {
      this.$emit('row-click', row, column, event)
    },
    onSizeChange: function (val) {
      this.currentPage = 1
      this.$emit('size-change', val)
      this.$emit('page-change', { page: 1, pageSize: val })
    },
    onPageChange: function (val) {
      this.$emit('page-change', { page: val, pageSize: this.pageSize })
    },
    /** 内置只读查看：打开通用详情弹窗 */
    openView: function (row) {
      this.viewRow = row
      this.viewVisible = true
    },
    /** 内置查看弹窗的单元格值格式化（复用全站 window.RgFormat 逻辑，空值显示 —） */
    formatViewValue: function (row, col) {
      var val = row ? row[col.prop] : undefined
      if (val === null || val === undefined || val === '') return '—'
      if (typeof col.formatter === 'function') return col.formatter(val, row, col)
      if (col.type === 'money') return '¥' + this.formatMoney(val)
      if (col.type === 'number') return this.formatNumber(val)
      return val
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
    /** 弹窗宽度（支持 sm/md/lg/xl/fullscreen 别名，或直接写 600px 等值） */
    size: {
      type: String,
      default: 'md'
    },
    /** 弹窗宽度（px，如 600px；优先级低于 size 别名） */
    width: {
      type: String,
      default: ''
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
    },
    /** 自定义额外 class（追加在 unified-dialog 之后，用于特殊场景样式覆盖） */
    customClass: {
      type: String,
      default: ''
    },
    /**
     * 未保存守卫（可选）：关闭前校验，防止误关丢失表单数据。
     * - 不传：保持原行为（直接关闭）。
     * - 传函数：返回 false 中止关闭；返回 Promise 时，resolve(false) 中止、其余放行。
     *   X 按钮、ESC、取消按钮均走此守卫；遮罩点击仍由 closeOnClickModal 控制。
     * 例：:before-close="() => !formDirty"
     */
    beforeClose: {
      type: Function,
      default: null
    }
  },
  data: function () {
    // 修改点：内部镜像 visible，避免直接变异 prop（X/ESC 关闭时 prop 变异无法回传父，导致弹窗关不掉/关了又开）
    return { currentVisible: this.visible }
  },
  watch: {
    // 父通过 .sync 更新 visible 时，同步到内部受控状态
    visible: function (val) { this.currentVisible = val }
  },
  mounted: function () {
    // 架构护栏：crud-dialog 必须受父级 .sync 或显式 @update:visible 控制。
    // 若父未监听 update:visible，关闭事件无法回传父，弹窗会"关不掉/关了又开"——
    // 正是本次修复的根因模式。挂载时主动告警，把"静默坏掉"变成"可见报错"，避免同类问题复发。
    if (typeof this.$listeners === 'undefined' || !this.$listeners['update:visible']) {
      console.error(
        '[crud-dialog] 缺少 "update:visible" 监听：使用处必须写 :visible.sync="xxx"' +
        '（或 :visible="xxx" @update:visible="xxx = $event"）。否则关闭后父状态不同步，弹窗将关不掉。'
      )
    }
  },
  computed: {
    /** 尺寸别名 → 标准像素宽度（当自定义 width 时优先使用 width） */
    resolvedWidth: function () {
      var sizeMap = { sm: '420px', md: '560px', lg: '720px', xl: '840px', fullscreen: '92%' }
      var s = (this.size || '').toLowerCase()
      return this.width || sizeMap[s] || '560px'
    },
    /** 是否全屏模式（fullscreen 时调整 top 和内边距） */
    isFullscreen: function () {
      return (this.size || '').toLowerCase() === 'fullscreen'
    },
    /** 是否使用了 header 插槽（用于条件渲染：有 header 插槽时不传 title 给 el-dialog） */
    hasHeaderSlot: function () {
      return !!this.$slots.header
    },
    /** 弹窗 class：unified-dialog + 尺寸别名（仅当未自定义 width 时）+ 自定义 class */
    dialogClass: function () {
      var cls = 'unified-dialog'
      if (!this.width) {
        cls += ' el-dialog--' + (this.size || 'md')
      }
      if (this.isFullscreen) {
        cls += ' el-dialog--fullscreen'
      }
      if (this.customClass) {
        cls += ' ' + this.customClass
      }
      return cls
    }
  },
  template:
    '<el-dialog' +
    '  show-close' +
    '  :custom-class="dialogClass"' +
    '  :title="hasHeaderSlot ? undefined : title"' +
    '  :visible="currentVisible"' +
    '  @update:visible="onVisibleChange"' +
    '  :width="resolvedWidth"' +
    '  :top="isFullscreen ? \'4vh\' : \'15vh\'"' +
    '  :close-on-click-modal="closeOnClickModal"' +
    '  :before-close="onBeforeClose"' +
    '  :close-on-press-escape="true"' +
    '  :destroy-on-close="true"' +
    '  :append-to-body="true"' +
    '  :modal-append-to-body="true"' +
    '>' +
      // 自定义头部插槽（覆盖默认 title，用于复杂头部如标签页切换）
      '<slot v-if="hasHeaderSlot" name="header"></slot>' +
      // 主体内容插槽
      '<slot></slot>' +
      // 底部按钮区
      '<span slot="footer" class="dialog-footer">' +
        '<slot name="footer">' +
          '<el-button v-if="showCancel" size="medium" @click="requestClose">{{ cancelText }}</el-button>' +
          '<el-button v-if="showSubmit" type="primary" size="medium" :loading="submitLoading" @click="$emit(\'submit\')">{{ submitText }}</el-button>' +
        '</slot>' +
      '</span>' +
    '</el-dialog>',
  methods: {
    handleClose: function () {
      this.currentVisible = false
      this.$emit('update:visible', false)
      this.$emit('close')
    },
    /**
     * 承接 el-dialog 的 update:visible：X 按钮 / ESC 关闭时由 onBeforeClose 调 done() 触发，
     * 这里把变化同步到内部状态并回传给父（.sync），确保父 visible 与弹窗实际状态一致。
     */
    onVisibleChange: function (val) {
      this.currentVisible = val
      this.$emit('update:visible', val)
      if (!val) { this.$emit('close') }
    },
    /**
     * 取消按钮 / 外部关闭请求：先跑未保存守卫，通过才真正关闭。
     * 守卫返回 false 中止；返回 Promise 时 resolve(false) 中止、其余放行。
     */
    requestClose: function () {
      var self = this
      if (typeof this.beforeClose === 'function') {
        var r = this.beforeClose()
        if (r && typeof r.then === 'function') {
          r.then(function (ok) { if (ok !== false) self.handleClose() })
          return
        }
        if (r === false) return
      }
      this.handleClose()
    },
    /**
     * 接入 el-dialog 原生 before-close：X 按钮与 ESC 关闭时触发。
     * 不调用 done() 即中止关闭（守卫返回 false 或 Promise resolve(false)）。
     */
    onBeforeClose: function (done) {
      var self = this
      if (typeof this.beforeClose === 'function') {
        var r = this.beforeClose()
        if (r && typeof r.then === 'function') {
          r.then(function (ok) { if (ok !== false) done() }).catch(function () { /* 异常时保持打开 */ })
          return
        }
        if (r === false) return
      }
      done()
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
// 修改点：F6 转义展示文本，防止配置值注入 HTML（代码表达式 onClick/model 等属受信配置不转义）
function escapeHtml(s) {
  if (s === null || s === undefined) return ''
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

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
      '<div class="stat-icon">' + escapeHtml(card.icon) + '</div>' +
      '<div class="stat-label">' + escapeHtml(card.label) + '</div>' +
      '<div class="stat-value">' + escapeHtml(card.value != null ? card.value : 0) + '</div>' +
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
      html += '<el-input v-model="' + item.model + '" placeholder="' + escapeHtml(item.placeholder) +
        '" style="width:' + width + '" clearable @keyup.enter.native="' + onEnter + '">' +
        '<i slot="prefix" class="el-input__icon el-icon-search" style="cursor:pointer" @click="' + onClick + '"></i>' +
        '</el-input>'
    } else if (item.type === 'select') {
      html += '<el-select v-model="' + item.model + '" placeholder="' + escapeHtml(item.placeholder) +
        '" style="width:' + width + ';margin-left:10px;" clearable @change="' + onChange + '">'
      if (item.options && item.options.length) {
        item.options.forEach(function (opt) {
          html += '<el-option label="' + escapeHtml(opt.label) + '" value="' + escapeHtml(opt.value != null ? opt.value : '') + '"></el-option>'
        })
      }
      html += '</el-select>'
    } else if (item.type === 'date') {
      // 修改点：添加 unlink-panels/editable，与新版 table-bar 组件保持一致
      html += '<el-date-picker v-model="' + item.model + '" clearable value-format="yyyy-MM-dd HH:mm:ss"' +
        ' type="datetimerange" format="yyyy-MM-dd HH:mm"' +
        ' unlink-panels :editable="false"' +
        ' placeholder="选择日期范围" range-separator="至"' +
        ' start-placeholder="开始日期" end-placeholder="结束日期"' +
        ' :default-time="[\'00:00:00\', \'23:59:59\']"' +
        ' style="width:' + width + ';margin-left:10px;" @change="' + onChange + '">' +
        '</el-date-picker>'
    }
  })

  if (actions.length > 0) {
    html += '<div class="tableLab">'
    actions.forEach(function (action) {
      var iconAttr = action.icon ? ' icon="' + escapeHtml(action.icon) + '"' : ''
      html += '<el-button type="' + (action.type || 'primary') + '" size="small" @click="' + action.onClick + '"' + iconAttr + '>' + escapeHtml(action.text) + '</el-button>'
    })
    html += '</div>'
  }

  html += '</div>'
  return html
}


// ============================================================
// 全局工具：单一来源集中在 window.RgFormat，新代码可直接引用
// （如 window.RgFormat.formatMoney），无需依赖全局 mixin
// ============================================================
// ============================================================
// 全局调色板：window.RgPalette —— ECharts 等 canvas 场景无法使用 CSS 变量，
// 故运行时从 tokens.css 读取令牌值，作为 JS 侧唯一颜色来源（M2 修复）。
// 用法：color: RgPalette.textSecondary / RgPalette.success ...
// ============================================================
window.RgPalette = (function () {
  function read(name, fallback) {
    try {
      var v = getComputedStyle(document.documentElement).getPropertyValue(name)
      return (v && v.trim()) ? v.trim() : fallback
    } catch (e) { return fallback }
  }
  return {
    // 文本
    textPrimary:   read('--el-text-primary', '#303133'),
    textRegular:   read('--el-text-regular', '#606266'),
    textSecondary: read('--el-text-secondary', '#909399'),
    textMuted:     read('--color-gray-400', '#9ca3af'),
    // 边框 / 背景
    borderDefault: read('--el-border-default', '#dcdfe6'),
    borderBase:    read('--el-border-base', '#c0c4cc'),
    borderLighter: read('--el-border-lighter', '#ebeef5'),
    borderLighter2: read('--el-border-lighter2', '#e4e7ed'),
    bgPage:        read('--bg-page', '#f5f6fa'),
    bgSubtle:      read('--color-gray-50', '#f9fafb'),
    white:         read('--color-white', '#ffffff'),
    // 语义色
    brand:         read('--color-brand-500', '#ffc200'),
    brandDark:     read('--color-brand-700', '#c99200'),
    success:       read('--color-success', '#67c23a'),
    warning:       read('--color-warning', '#e6a23c'),
    danger:        read('--color-danger', '#f56c6c'),
    info:          read('--color-info', '#909399'),
    // ECharts 常用数据系列色（数据可视化专用，非主题色）
    seriesBlue:    '#5470c6',
    seriesGreen:   '#91cc75',
    seriesOrange:  '#fc8452',
    seriesRed:     '#ff4d4f',
    seriesPurple:  '#722ed1',
    seriesCyan:    '#36a3f5',
    // 报表页额外用到的数据色 / 品牌色（单一来源，页面禁写 hex）
    alipayBlue:    '#1677FF',
    wechatGreen:   '#07C160',
    linkBlue:      '#409eff',
    successDark:   '#67a94e',
    successDeep:   '#3a8b1f',
    seriesGold:    '#fac858',
    seriesPink:    '#ee6666',
    seriesViolet:  '#9a60b4',
    seriesSky:     '#73c0de',
    seriesMagenta: '#ea7ccc',
    seriesGreen2:  '#13ce66',
    antdBlue:      '#1890ff',
    antdOrange:    '#ff7a45',
    brand600:      '#e6ae00',
    brand800:      '#a67600',
    // 圆角令牌（ECharts canvas 无法使用 CSS var，运行时从 tokens.css 读取）
    radiusSm:      parseInt(read('--radius-sm', '4'), 10),
    radiusMd:      parseInt(read('--radius-md', '8'), 10),
    radiusLg:      parseInt(read('--radius-lg', '12'), 10),
    // Member 中心图表用色（Tailwind 风格语义色）
    emerald:       '#22c55e',
    orange500:     '#f97316',
    blue500:       '#3b82f6',
    amber500:      '#f59e0b',
    violet500:     '#8b5cf6',
    // 半透明区域色（ECharts areaStyle 专用，无法用 CSS 变量直接读取后合成）
    seriesBlueArea:  'rgba(84,112,198,0.15)',
    seriesGreenArea: 'rgba(145,204,117,0.15)',
    successArea:     'rgba(103,194,58,0.15)',
    emeraldArea:     'rgba(34,197,94,0.15)',
    orange500Area:   'rgba(249,115,22,0.15)',
    // 会员等级进度条配色（Tailwind 风格，供 level-list.html 的 getBarColor 使用）
    slate200:      '#e2e8f0',
    violet400:     '#a78bfa',
    indigo400:     '#818cf8',
    blue400:       '#60a5fa',
    emerald400:    '#34d399',
    amber400:      '#fbbf24',
    red500:        '#ef4444',
    gray999:       '#999999',
    bgLight:       '#f2f3f5',
    blueSoft:      '#f0f4ff',
    blueSoft2:     '#a0cfff',
    blueDark:      '#2b6cb0',
    blueDarkest:   '#1a365d',
    graySoft:      '#d4d7de'
  }
})()
window.RgFormat = {
  /** 金额格式化：千分位 + 保留两位小数，空值返回 0.00（全站金额展示统一入口） */
  formatMoney: function (val) {
    if (val === null || val === undefined || val === '') return '0.00'
    var n = Number(val)
    if (isNaN(n)) return '0.00'
    var fixed = n.toFixed(2)
    var parts = fixed.split('.')
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    return parts.join('.')
  },
  /** 数字格式化：千分位（保留原小数位），空值返回 ''，用于数量/积分/库存等列 */
  formatNumber: function (val) {
    if (val === null || val === undefined || val === '') return ''
    var n = Number(val)
    if (isNaN(n)) return String(val)
    var s = String(n)
    var parts = s.split('.')
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    return parts.join('.')
  },
  /** 日期格式化：截取 yyyy-MM-dd，空值返回 '-' */
  formatDate: function (val) {
    if (!val) return '-'
    var s = String(val)
    return s.length >= 10 ? s.substring(0, 10) : s
  },
  /** 时间格式化：原样返回，空值返回 '-' */
  formatDateTime: function (val) {
    return val ? String(val) : '-'
  },
  /** 状态中文：mapName 对应 ReggieStatus.register 注册的名称 */
  rgStatusText: function (mapName, status) {
    return window.ReggieStatus ? window.ReggieStatus.text(mapName, status) : ''
  },
  /** 状态标签类型：返回 Element tag type（warning/primary/...） */
  rgStatusTag: function (mapName, status) {
    return window.ReggieStatus ? window.ReggieStatus.tag(mapName, status) : 'info'
  },
  /** 构造 el-image 的 preview-src-list（避免各页重复拼接数组） */
  rgPreview: function (url) {
    return url ? [url] : []
  }
}

// 修改点：F7 全局 mixin 委托到 window.RgFormat（保持 51 页 this.formatXxx 兼容，
// 同时统一逻辑来源，后续可逐步迁移到 window.RgFormat 并移除此 mixin 以降低全局污染）
Vue.mixin({
  methods: {
    formatMoney: function (val) { return window.RgFormat.formatMoney(val) },
    formatNumber: function (val) { return window.RgFormat.formatNumber(val) },
    formatDate: function (val) { return window.RgFormat.formatDate(val) },
    formatDateTime: function (val) { return window.RgFormat.formatDateTime(val) },
    rgStatusText: function (mapName, status) { return window.RgFormat.rgStatusText(mapName, status) },
    rgStatusTag: function (mapName, status) { return window.RgFormat.rgStatusTag(mapName, status) },
    rgPreview: function (url) { return window.RgFormat.rgPreview(url) }
  }
})


// ============================================================
// 状态枚举注册中心：消除各页重复定义 statusMap / typeMap 的问题
// 用法（页面内）：
//   window.ReggieStatus.register('order', textMap, tagMap)
//   <el-tag :type="rgStatusTag('order', row.status)">{{ rgStatusText('order', row.status) }}</el-tag>
// ============================================================
window.ReggieStatus = {
  _maps: {},
  /** 注册某业务的状态映射，textMap: {status: 中文}, tagMap: {status: tagType} */
  register: function (name, textMap, tagMap) {
    this._maps[name] = { text: textMap || {}, tag: tagMap || {} }
  },
  /** 取状态中文，未注册或缺失返回 '' */
  text: function (name, status) {
    var m = this._maps[name]
    return m && m.text[status] != null ? m.text[status] : ''
  },
  /** 取状态 tag 类型，未注册或缺失返回 'info' */
  tag: function (name, status) {
    var m = this._maps[name]
    return m && m.tag[status] != null ? m.tag[status] : 'info'
  }
}
// 预置：订单状态（与 order/list.html 既有映射一致，供订单/支付/配送页复用）
window.ReggieStatus.register('order',
  { 1: '待付款', 2: '待接单', 3: '配送中', 4: '已完成', 5: '已取消', 6: '已退款' },
  { 1: 'warning', 2: 'warning', 3: 'primary', 4: 'success', 5: 'info', 6: 'danger' }
)


// ============================================================
// 列表页通用 Mixin：消除分页样板（page/pageSize/counts + 翻页/改大小处理）
// 用法（页面内）：
//   new Vue({ mixins: [window.ReggieListMixin], methods: { fetchData() { /* 读 this.page/this.pageSize 请求 */ } } })
//   <crud-table ... @page-change="onPageChange" @size-change="onSizeChange" />
// ============================================================
window.ReggieListMixin = {
  data: function () {
    return {
      page: 1,
      pageSize: 10,
      counts: 0,
      loading: false
    }
  },
  methods: {
    /** 页码变化（来自 crud-table 的 page-change 事件） */
    onPageChange: function (payload) {
      this.page = payload.page
      if (typeof this.fetchData === 'function') this.fetchData()
    },
    /** 每页条数变化（来自 crud-table 的 size-change 事件）
     *  修改点：crud-table emit 的是纯数字 val，而非 {pageSize: number}，需兼容两种格式 */
    onSizeChange: function (payload) {
      this.page = 1
      // 修改点：兼容 crud-table 传纯数字和 table-bar 传对象两种情况
      this.pageSize = (typeof payload === 'object' && payload != null) ? (payload.pageSize || payload) : payload
      // 不在此调用 fetchData：crud-table 改页大小时会同步连续 emit size-change 再 emit
      // page-change（components.js onSizeChange），紧随的 onPageChange 会以 page=1 统一加载。
      // 若这里也 fetch，改页大小必然发出两次相同请求。
    }
  }
}


// ============================================================
// 组件：rg-image-uploader — 图片/凭证上传（多图 或 单图头像）
//   v-model 绑定逗号分隔的相对路径；上传走 /common/upload，回显走 /common/download
//   props:
//     bizType  业务目录(dish/purchase/stockcheck/stockrecord/supplier/tenant/avatar)
//     max      多图最多张数(默认5)
//     single   单图模式(头像/单凭证)
//     round    圆形预览(配合 single，用于头像)
//     disabled 只读
// ============================================================
Vue.component('rg-image-uploader', {
  props: {
    value: { type: String, default: '' },
    bizType: { type: String, default: 'dish' },
    max: { type: Number, default: 5 },
    single: { type: Boolean, default: false },
    round: { type: Boolean, default: false },
    disabled: { type: Boolean, default: false }
  },
  data: function () {
    return { fileList: [] }
  },
  computed: {
    uploadHeaders: function () {
      // el-upload 绕过 axios，需手动带 CSRF Token（生产 CsrfFilter 校验）
      var token = (typeof window.getCsrfToken === 'function') ? window.getCsrfToken() : ''
      return { 'X-CSRF-Token': token || '' }
    },
    // 单图模式当前图片的完整回显地址
    singleUrl: function () {
      var rel = this.relativeList()[0]
      return rel ? imgPath(rel) : ''
    }
  },
  created: function () {
    this.fileList = this.buildFileList(this.value)
  },
  watch: {
    value: function (val) {
      // 仅当外部值与内部值不一致时重建（避免上传成功 emit 后重复重建）
      if (this.normalize(val) !== this.normalize(this.currentValue())) {
        this.fileList = this.buildFileList(val)
      }
    }
  },
  template:
    '<div class="rg-image-uploader">' +
      // 多图：picture-card 宫格
      '<el-upload v-if="!single" list-type="picture-card" action="/common/upload" name="file"' +
        ' :data="{ bizType: bizType }" :headers="uploadHeaders" :file-list="fileList"' +
        ' :disabled="disabled"' +
        ' :on-success="handleSuccess" :on-remove="handleRemove" :on-preview="handlePreview"' +
        ' :on-error="handleError" :before-upload="beforeUpload"' +
        ' :limit="max" :on-exceed="onExceed">' +
        '<i class="el-icon-plus"></i>' +
      '</el-upload>' +
      // 单图：复用全局 avatar-uploader 样式（圆形用于头像）；点击图片可重新选择覆盖，右上角可清除
      '<div v-else class="avatar-uploader" style="position:relative;display:inline-block;width:120px;">' +
        '<el-upload action="/common/upload" name="file"' +
          ' :data="{ bizType: bizType }" :headers="uploadHeaders" :show-file-list="false"' +
          ' :disabled="disabled"' +
          ' :on-success="handleSuccess" :on-error="handleError" :before-upload="beforeUpload">' +
          '<img v-if="singleUrl" :src="singleUrl" class="avatar"' +
            ' :style="round ? { borderRadius: \'50%\' } : {}" alt="上传图片">' +
          '<i v-else class="el-icon-plus avatar-uploader-icon"></i>' +
        '</el-upload>' +
        '<span v-if="singleUrl && !disabled" @click="clearSingle" title="移除图片"' +
          ' style="position:absolute;top:-4px;right:-4px;width:18px;height:18px;line-height:14px;text-align:center;' +
          'background:var(--text-muted);color:var(--bg-surface);border:2px solid var(--bg-surface);' +
          'border-radius:50%;font-size:12px;cursor:pointer;">×</span>' +
      '</div>' +
    '</div>',
  methods: {
    /** 逗号分隔字符串 → el-upload 回显列表 */
    buildFileList: function (val) {
      var list = []
      String(val || '').split(',').forEach(function (p) {
        var rel = p.trim()
        if (rel) {
          var seg = rel.split('/')
          list.push({ name: seg[seg.length - 1], url: imgPath(rel), relative: rel, status: 'success' })
        }
      })
      return list
    },
    /** 当前已成功上传的相对路径数组 */
    relativeList: function () {
      var out = []
      this.fileList.forEach(function (f) { if (f.relative) out.push(f.relative) })
      return out
    },
    currentValue: function () {
      return this.relativeList().join(',')
    },
    /** 归一化仅用于"外部值 vs 内部值"比较 */
    normalize: function (s) {
      return String(s || '').split(',').map(function (x) { return x.trim() }).filter(Boolean).join('|')
    },
    syncValue: function (fileList) {
      this.fileList = fileList
      this.$emit('input', this.currentValue())
    },
    /** 上传前校验类型与大小（后端二次校验，这里提前拦截给即时反馈） */
    beforeUpload: function (file) {
      var suffix = file.name.split('.').pop().toLowerCase()
      if (['jpg', 'jpeg', 'png', 'gif'].indexOf(suffix) < 0) {
        this.$message.error('仅支持 jpg、jpeg、png、gif 格式')
        return false
      }
      if (file.size > 5 * 1024 * 1024) {
        this.$message.error('文件大小不能超过 5MB')
        return false
      }
      return true
    },
    handleSuccess: function (response, file, fileList) {
      if (!response || String(response.code) !== '1' || !response.data) {
        if (response && response.msg === 'NOTLOGIN') { this.notifyNotLogin(); return }
        this.$message.error((response && response.msg) || '图片上传失败，请重试')
        this.removeFailed(file, fileList)
        return
      }
      file.relative = response.data
      file.url = imgPath(response.data)
      // 单图模式：只保留最新一张
      this.syncValue(this.single ? [file] : fileList)
    },
    handleRemove: function (file, fileList) {
      this.syncValue(fileList)
    },
    /** 单图清除 */
    clearSingle: function () {
      this.syncValue([])
    },
    handlePreview: function (file) {
      if (file.url) window.open(file.url, '_blank')
    },
    handleError: function (err) {
      var msg = (err && err.message) || '图片上传失败，请重试'
      // 仅 401（未登录/登录过期）才通知父窗口跳登录，避免服务端 500、网络抖动误踢用户
      if (msg === 'Request failed with status code 401') {
        this.notifyNotLogin()
        this.$message.error('登录已过期，请重新登录')
        return
      }
      this.$message.error(msg)
    },
    onExceed: function () {
      this.$message.warning(this.single ? '只能上传 1 张图片' : ('最多上传 ' + this.max + ' 张图片'))
    },
    /** 业务失败时把该文件从列表剔除 */
    removeFailed: function (file, fileList) {
      var self = this
      this.$nextTick(function () {
        var idx = fileList.indexOf(file)
        if (idx >= 0) fileList.splice(idx, 1)
        self.syncValue(fileList)
      })
    },
    notifyNotLogin: function () {
      if (window.self !== window.top) {
        try { window.parent.postMessage({ type: 'REGGIE_NOTLOGIN' }, '*') } catch (e) {}
      } else {
        window.location.href = '/backend/page/login/login.html'
      }
    }
  }
})
