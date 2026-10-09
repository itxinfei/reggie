/* ============================================================
 * 瑞吉外卖 C 端 — 跨页面复用组件（Vue 2 / Vant 2 / 无构建）
 *
 * 组件清单：
 *   <cend-shell>      页面壳：导航栏 + 内容槽 + 底部 Tab
 *   <cend-empty>      空态：图标 + 文本 + 副文本 + 操作按钮
 *
 * 导航主题：light（白底轻导航，对标美团二级页，默认）/ brand（品牌金，首页类）
 *
 * 引入方式（在所有 C 页面 vant.min.js 之后、new Vue 之前）：
 *   <script src="../js/components-cend.js"></script>
 *
 * @since 2026-09-22
 * ============================================================ */

(function () {
  'use strict';

  // ─── 校验 Vue 已加载（导航/空态本身不依赖 Vant；van-tabbar 仅在 showTabbar 时才渲染）───
  if (typeof Vue === 'function') {
    initComponents();
  } else {
    // head 中 components-cend.js 在 vue 之前引入时延迟注册
    window.addEventListener('DOMContentLoaded', function () {
      if (typeof Vue === 'function') initComponents();
    });
  }

  function initComponents() {

    /* ==========================================================
     * <cend-shell> — 页面壳
     * ==========================================================
     * Props：
     *   title        [string] 标题（默认空）
     *   light        [bool]   true=白底轻导航(默认) false=品牌金导航
     *   show-back    [bool]   是否显示左返回箭头（默认 true）
     *   show-home    [bool]   是否显示右侧首页图标（默认 false）
     *   show-tabbar  [bool]   是否显示底部 Tab（默认 false）
     *   tab-active   [number] 当前 Tab 索引（0=点餐/1=订单/2=我的，默认 0）
     *
     * Events：@back / @home / @tab-change
     * Slot：default — 主内容区域
     */
    Vue.component('cend-shell', {
      template: [
        '<div class="cend-page">',
        '  <!-- 导航栏：独立 .cend-navbar，不复用各页 .divHead 金色作用域样式 -->',
        '  <div v-if="title || showBack || showHome || $slots.right" class="cend-navbar" :class="light ? \'is-light\' : \'is-brand\'">',
        '    <i v-if="showBack" class="cend-navbar__btn cend-navbar__back ri-arrow-left-s-line" @click="onBack"></i>',
        '    <span class="cend-navbar__title">{{ title }}</span>',
        '    <i v-if="showHome && !$slots.right" class="cend-navbar__btn cend-navbar__home ri-home-4-line" @click="onHome"></i>',
        '    <span v-if="$slots.right" class="cend-navbar__right"><slot name="right"></slot></span>',
        '  </div>',
        '  <!-- 主内容（页面自行控制滚动和间距） -->',
        '  <slot></slot>',
        '  <!-- 底部 Tab（Vant 自带 fixed 定位 + 安全区） -->',
        '  <van-tabbar v-if="showTabbar" :value="tabActive" :safe-area-inset="true" @change="onTabChange" class="app-tabbar">',
        '    <van-tabbar-item><span>点餐</span><i slot="icon" class="ri-restaurant-line"></i></van-tabbar-item>',
        '    <van-tabbar-item><span>订单</span><i slot="icon" class="ri-file-list-line"></i></van-tabbar-item>',
        '    <van-tabbar-item><span>我的</span><i slot="icon" class="ri-user-3-line"></i></van-tabbar-item>',
        '  </van-tabbar>',
        '</div>'
      ].join(''),
      props: {
        title:      { type: String, default: '' },
        light:      { type: Boolean, default: true },
        showBack:   { type: Boolean, default: true },
        showHome:   { type: Boolean, default: false },
        showTabbar: { type: Boolean, default: false },
        tabActive:  { type: Number, default: 0 }
      },
      methods: {
        onBack: function () {
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
        onHome: function () {
          window.location.href = '/front/index.html';
          this.$emit('home');
        },
        onTabChange: function (active) {
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
     * Props：
     *   icon  [string] RemixIcon 类名 或 图片 URL（自动判断）
     *   text  [string] 主文案（默认 "暂无数据"）
     *   sub   [string] 副文案（可选）
     *   action-text [string] 操作按钮文案（可选，不传不显示按钮）
     * Events：@action
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
        isImg: function () {
          return this.icon && (this.icon.indexOf('/') === 0 || this.icon.indexOf('http') === 0 || this.icon.indexOf('./') === 0);
        }
      },
      methods: {
        onAction: function () {
          this.$emit('action');
        }
      }
    });

  } // initComponents

})();
