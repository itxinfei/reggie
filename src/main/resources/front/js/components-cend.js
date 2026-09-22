/* ============================================================
 * 瑞吉外卖 C 端 — 跨页面复用组件（Vue 2 / Vant 2 / 无构建）
 *
 * 组件清单：
 *   <cend-shell>      页面壳：头 + 内容槽 + 底部 Tab
 *   <cend-empty>      空态：图标 + 文本 + 副文本 + 操作按钮
 *
 * 待补充（后续阶段）：<cend-order-card> 订单卡片（头 + 菜品 + 合计 + 操作按钮）
 *
 * 引入方式（在所有 C 页面 <script src="../js/vant.min.js"> 之后）：
 *   <script src="../js/components-cend.js"></script>
 *
 * @since 2026-09-22
 * ============================================================ */

(function () {
  'use strict';

  // ─── 校验 Vue/Vant 已加载 ───
  if (typeof Vue === 'function' && typeof vant === 'object') {
    initComponents();
  } else {
    // head 中 components-cend.js 在 vant 之前引入时延迟注册
    window.addEventListener('DOMContentLoaded', function () {
      if (typeof Vue === 'function' && typeof vant === 'object') initComponents();
    });
  }

  function initComponents() {

    /* ==========================================================
     * <cend-shell> — 页面壳
     * ==========================================================
     * 取代每个页面重复的 <div class="divHead"> + 可选 <van-tabbar>。
     *
     * Props：
     *   title        [string]   标题（默认空）
     *   show-back    [bool]     是否显示左返回箭头（默认 true）
     *   show-home    [bool]     是否显示右侧首页图标（默认 false）
     *   show-tabbar  [bool]     是否显示底部 Tab（默认 false）
     *   tab-active   [number]   当前 Tab 索引（0=点餐/1=订单/2=我的，默认 0）
     *
     * Events：
     *   @back  点击返回箭头时触发（默认调用 common.goBack()）
     *   @home  点击首页图标时触发
     *   @tab-change  Tab 切换时触发
     *
     * Slot：default — 主内容区域
     */
    Vue.component('cend-shell', {
      template: [
        '<div>',
        '  <!-- 头部 -->',
        '  <div class="divHead" v-if="title || showBack || showHome">',
        '    <div class="divTitle">',
        '      <i v-if="showBack" class="ri-arrow-left-s-line" @click="onBack"></i>',
        '      <span>{{ title }}</span>',
        '    </div>',
        '    <div class="divHead-right">',
        '      <i v-if="showHome" class="ri-home-4-line" @click="onHome"></i>',
        '    </div>',
        '  </div>',
        '  <!-- 主内容（页面自行控制滚动和间距） -->',
        '  <slot></slot>',
        '  <!-- 底部 Tab（Vant 自带 fixed 定位） -->',
        '  <van-tabbar v-if="showTabbar" :value="tabActive" @change="onTabChange" class="app-tabbar">',
        '    <van-tabbar-item><span>点餐</span><i slot="icon" class="ri-restaurant-line"></i></van-tabbar-item>',
        '    <van-tabbar-item><span>订单</span><i slot="icon" class="ri-file-list-line"></i></van-tabbar-item>',
        '    <van-tabbar-item><span>我的</span><i slot="icon" class="ri-user-3-line"></i></van-tabbar-item>',
        '  </van-tabbar>',
        '</div>'
      ].join(''),
      props: {
        title:        { type: String, default: '' },
        showBack:     { type: Boolean, default: true },
        showHome:     { type: Boolean, default: false },
        showTabbar:   { type: Boolean, default: false },
        tabActive:    { type: Number, default: 0 }
      },
      methods: {
        onBack() {
          // 优先使用全局兜底，避免页面自定义方法覆盖导致行为不统一
          if (typeof window.goBack === 'function') {
            window.goBack();
          } else if (history.length > 1) {
            history.go(-1);
          } else {
            window.location.href = '/front/index.html';
          }
          this.$emit('back');
        },
        onHome() {
          window.location.href = '/front/index.html';
          this.$emit('home');
        },
        onTabChange(active) {
          if (active === this.tabActive) return;
          var urlMap = { 0: '/front/index.html', 1: '/front/page/order.html', 2: '/front/page/user.html' };
          var url = urlMap[active];
          if (url) {
            this.$emit('tab-change', active);
            window.location.href = url;
          }
        }
      }
    });

    /* ==========================================================
     * <cend-empty> — 空态
     * ==========================================================
     * 取代分散在各页面的空态 HTML（部分页面用 PNG、部分用 RemixIcon）。
     *
     * Props：
     *   icon  [string]   RemixIcon 类名 或 图片 URL（自动判断）
     *   text  [string]   主文案（默认 "暂无数据"）
     *   sub   [string]   副文案（可选）
     *   action-text  [string]   操作按钮文案（可选，不传不显示按钮）
     *
     * Events：
     *   @action  点击操作按钮时触发
     */
    Vue.component('cend-empty', {
      template: [
        '<div class="empty-state has-tabbar">',
        '  <img v-if="isImg" :src="icon" :alt="text"/>',
        '  <i v-else :class="icon"></i>',
        '  <p class="empty-text">{{ text }}</p>',
        '  <span class="empty-sub">{{ sub }}</span>',
        '  <div v-if="actionText" class="empty-action" @click="onAction">{{ actionText }}</div>',
        '</div>'
      ].join(''),
      props: {
        icon:       { type: String, default: 'ri-inbox-line' },
        text:       { type: String, default: '暂无数据' },
        sub:        { type: String, default: '' },
        actionText: { type: String, default: '' }
      },
      computed: {
        isImg() {
          return this.icon && (this.icon.indexOf('/') === 0 || this.icon.indexOf('http') === 0 || this.icon.indexOf('./') === 0);
        }
      },
      methods: {
        onAction() {
          this.$emit('action');
        }
      }
    });

  } // initComponents

})();
