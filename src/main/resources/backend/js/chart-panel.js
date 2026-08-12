/**
 * 报表页通用组件
 * 适用于 iframe 架构 + Vue 2 + Element UI 的无构建工具场景
 *
 * 组件列表：
 *   <chart-panel>    — 图表面板（统一圆角、阴影、标题栏、hover 效果）
 *   <kpi-card>       — KPI 指标卡片（统一颜色主题、图标布局）
 *   <page-shell>     — 报表页外壳（统一 filter-bar + stats-cards 布局）
 *
 * 使用方式：
 *   1. 引入样式：<link rel="stylesheet" href="../../styles/chart-panel.css" />
 *   2. 引入组件：<script src="../../js/chart-panel.js"></script>
 *   3. 在 Vue 模板中使用：
 *      <page-shell title="营业日报" :cards="statsCards">
 *        <template #filter><slot name="filter"></slot></template>
 *        <chart-panel title="订单趋势" style="margin-bottom:20px;">
 *          <div id="chart1" style="width:100%;height:380px;"></div>
 *        </chart-panel>
 *      </page-shell>
 *
 * @author Reggie Team
 * @since 2026-07-16
 */

// ============================================================
// 组件1：chart-panel — 图表面板
// ============================================================
Vue.component('chart-panel', {
  props: {
    /** 面板标题 */
    title: {
      type: String,
      default: ''
    },
    /** 标题前缀图标（emoji 或文本） */
    icon: {
      type: String,
      default: ''
    },
    /** 面板内边距 */
    padding: {
      type: String,
      default: '24px'
    },
    /** 底部间距 */
    marginBottom: {
      type: String,
      default: '20px'
    }
  },
  template:
    '<div class="chart-panel" :style="{ padding: padding, marginBottom: marginBottom }">' +
      '<div v-if="title || $slots.title" class="chart-panel__title">' +
        '<slot name="title">' +
          '<span v-if="icon" class="chart-panel__icon" v-text="icon"></span>' +
          '<span>{{ title }}</span>' +
        '</slot>' +
      '</div>' +
      '<div class="chart-panel__body">' +
        '<slot></slot>' +
      '</div>' +
    '</div>'
})


// ============================================================
// 组件2：kpi-card — KPI 指标卡片
// ============================================================
Vue.component('kpi-card', {
  props: {
    /** 数值 */
    value: {
      type: [Number, String],
      default: 0
    },
    /** 标签 */
    label: {
      type: String,
      default: ''
    },
    /** 颜色主题：primary / success / warning / danger / info / purple */
    color: {
      type: String,
      default: 'primary'
    },
    /** 单位 */
    unit: {
      type: String,
      default: ''
    },
    /** 图标（emoji 或文本） */
    icon: {
      type: String,
      default: ''
    },
    /** 是否使用 flex 行布局（图标在左，文字在右） */
    flex: {
      type: Boolean,
      default: false
    },
    /** 附加提示文字 */
    subText: {
      type: String,
      default: ''
    }
  },
  computed: {
    cardClass: function () {
      return 'kpi-card kpi-card--' + (this.color || 'primary')
    }
  },
  template:
    '<div :class="cardClass">' +
      '<div v-if="icon" class="kpi-card__icon" v-text="icon"></div>' +
      '<div class="kpi-card__content">' +
        '<div class="kpi-card__label">{{ label }}</div>' +
        '<div class="kpi-card__value">' +
          '{{ value != null ? value : 0 }}' +
          '<small v-if="unit" class="kpi-card__unit">{{ unit }}</small>' +
        '</div>' +
        '<div v-if="subText" class="kpi-card__sub">{{ subText }}</div>' +
      '</div>' +
    '</div>'
})


// ============================================================
// 组件3：page-shell — 报表页外壳
// ============================================================
Vue.component('page-shell', {
  props: {
    /** 页面标题 */
    title: {
      type: String,
      default: ''
    },
    /** KPI 卡片配置 [{ value, label, color, icon, unit }] */
    cards: {
      type: Array,
      default: function () { return [] }
    },
    /** 卡片列数：3 或 4（默认 4） */
    cardCols: {
      type: Number,
      default: 4
    }
  },
  template:
    '<div class="page-shell">' +
      '<div v-if="title" class="page-shell__header">{{ title }}</div>' +
      '<div class="page-shell__toolbar"><slot name="toolbar"></slot></div>' +
      '<div v-if="cards.length > 0" class="page-shell__cards" :class="cardsColClass">' +
        '<kpi-card v-for="card in cards" :key="card.label || card.value"' +
          ' :value="card.value" :label="card.label" :color="card.color"' +
          ' :icon="card.icon" :unit="card.unit || \'\'"' +
          ' :sub-text="card.subText || \'\'"' +
          ' :flex="!!card.flex" />' +
      '</div>' +
      '<div class="page-shell__content"><slot></slot></div>' +
    '</div>',
  computed: {
    cardsColClass: function () {
      return this.cardCols === 3 ? 'page-shell__cards--col-3' : ''
    }
  }
})
