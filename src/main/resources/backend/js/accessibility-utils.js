/**
 * 无障碍访问工具函数
 * 提供aria属性和无障碍访问支持
 */
const AccessibilityUtils = {
  /**
   * 为表格添加无障碍属性
   * @param {HTMLElement} table - 表格元素
   * @param {string} tableName - 表格名称
   */
  addTableAccessibility(table, tableName) {
    if (!table) return;
    
    table.setAttribute('role', 'table');
    table.setAttribute('aria-label', tableName);
    
    // 为表头添加无障碍属性
    const headers = table.querySelectorAll('th');
    headers.forEach((header, index) => {
      header.setAttribute('role', 'columnheader');
      header.setAttribute('scope', 'col');
    });
    
    // 为表格行添加无障碍属性
    const rows = table.querySelectorAll('tbody tr');
    rows.forEach((row, index) => {
      row.setAttribute('role', 'row');
      row.setAttribute('aria-rowindex', index + 1);
    });
  },
  
  /**
   * 为表单添加无障碍属性
   * @param {HTMLElement} form - 表单元素
   * @param {string} formName - 表单名称
   */
  addFormAccessibility(form, formName) {
    if (!form) return;
    
    form.setAttribute('role', 'form');
    form.setAttribute('aria-label', formName);
    
    // 为表单元素添加无障碍属性
    const inputs = form.querySelectorAll('input, select, textarea');
    inputs.forEach(input => {
      const label = input.closest('.el-form-item')?.querySelector('label');
      if (label) {
        const labelId = 'label-' + Math.random().toString(36).substr(2, 9);
        label.id = labelId;
        input.setAttribute('aria-labelledby', labelId);
      }
      
      if (input.hasAttribute('required')) {
        input.setAttribute('aria-required', 'true');
      }
      
      if (input.hasAttribute('disabled')) {
        input.setAttribute('aria-disabled', 'true');
      }
    });
  },
  
  /**
   * 为弹窗添加无障碍属性
   * @param {HTMLElement} dialog - 弹窗元素
   * @param {string} dialogName - 弹窗名称
   */
  addDialogAccessibility(dialog, dialogName) {
    if (!dialog) return;
    
    dialog.setAttribute('role', 'dialog');
    dialog.setAttribute('aria-modal', 'true');
    dialog.setAttribute('aria-label', dialogName);
    
    // 为关闭按钮添加无障碍属性
    const closeBtn = dialog.querySelector('.el-dialog__headerbtn');
    if (closeBtn) {
      closeBtn.setAttribute('aria-label', '关闭');
    }
  },
  
  /**
   * 为按钮添加无障碍属性
   * @param {HTMLElement} button - 按钮元素
   * @param {string} buttonName - 按钮名称
   */
  addButtonAccessibility(button, buttonName) {
    if (!button) return;
    
    button.setAttribute('aria-label', buttonName);
    
    // 如果按钮有图标，添加图标描述
    const icon = button.querySelector('i, .el-icon-');
    if (icon) {
      icon.setAttribute('aria-hidden', 'true');
    }
  },
  
  /**
   * 为图片添加无障碍属性
   * @param {HTMLElement} img - 图片元素
   * @param {string} altText - 替代文本
   */
  addImageAccessibility(img, altText) {
    if (!img) return;
    
    img.setAttribute('alt', altText);
    img.setAttribute('role', 'img');
  },
  
  /**
   * 为导航添加无障碍属性
   * @param {HTMLElement} nav - 导航元素
   * @param {string} navName - 导航名称
   */
  addNavigationAccessibility(nav, navName) {
    if (!nav) return;
    
    nav.setAttribute('role', 'navigation');
    nav.setAttribute('aria-label', navName);
    
    // 为导航链接添加无障碍属性
    const links = nav.querySelectorAll('a');
    links.forEach(link => {
      link.setAttribute('role', 'link');
    });
  },
  
  /**
   * 为搜索框添加无障碍属性
   * @param {HTMLElement} searchInput - 搜索框元素
   * @param {string} searchName - 搜索框名称
   */
  addSearchAccessibility(searchInput, searchName) {
    if (!searchInput) return;
    
    searchInput.setAttribute('role', 'searchbox');
    searchInput.setAttribute('aria-label', searchName);
    searchInput.setAttribute('aria-autocomplete', 'list');
  },
  
  /**
   * 为分页添加无障碍属性
   * @param {HTMLElement} pagination - 分页元素
   * @param {string} paginationName - 分页名称
   */
  addPaginationAccessibility(pagination, paginationName) {
    if (!pagination) return;
    
    pagination.setAttribute('role', 'navigation');
    pagination.setAttribute('aria-label', paginationName);
    
    // 为分页按钮添加无障碍属性
    const buttons = pagination.querySelectorAll('button');
    buttons.forEach(button => {
      button.setAttribute('aria-label', button.textContent || '分页按钮');
    });
  }
};

// 导出工具函数
if (typeof module !== 'undefined' && module.exports) {
  module.exports = AccessibilityUtils;
}

/* ============================================================
 * P0 可访问性修复（2026-09-27）：为无名表单控件自动补可访问名
 * ------------------------------------------------------------
 * 背景：全站审查发现 193 处 input 无可访问名称——
 *   1) el-table 行选择 checkbox（el-checkbox__original，无任何名字）
 *   2) el-pagination 跳页输入框（type=number，无 label/placeholder）
 * 本段自执行：DOMContentLoaded 后修复 + MutationObserver 兜底
 * （Vue 异步渲染/翻页重建 DOM 后依然生效），幂等不重复添加。
 * ============================================================ */
(function () {
  'use strict';

  function nameAnonymousControls(root) {
    try {
      // 1) 表头全选 checkbox
      var headerBoxes = root.querySelectorAll('.el-table__header-wrapper .el-checkbox__original');
      for (var i = 0; i < headerBoxes.length; i++) {
        if (!headerBoxes[i].getAttribute('aria-label')) {
          headerBoxes[i].setAttribute('aria-label', '全选本页');
        }
      }
      // 2) 表体行选择 checkbox
      var bodyBoxes = root.querySelectorAll('.el-table__body-wrapper .el-checkbox__original');
      for (var j = 0; j < bodyBoxes.length; j++) {
        if (!bodyBoxes[j].getAttribute('aria-label')) {
          bodyBoxes[j].setAttribute('aria-label', '选择此行');
        }
      }
      // 2b) 其余无名 el-checkbox__original：优先取同组 label 文本，否则通用名
      var anyBoxes = root.querySelectorAll('.el-checkbox__original');
      for (var j2 = 0; j2 < anyBoxes.length; j2++) {
        var ab = anyBoxes[j2];
        if (ab.getAttribute('aria-label') || ab.getAttribute('aria-labelledby')) continue;
        var wrap = ab.closest('.el-checkbox');
        var lblEl = wrap ? wrap.querySelector('.el-checkbox__label') : null;
        var lt = lblEl ? (lblEl.textContent || '').trim() : '';
        ab.setAttribute('aria-label', lt ? '选择：' + lt.slice(0, 20) : '选择此行');
      }
      // 3) 分页跳转输入框（Element UI jumper）
      var jumpInputs = root.querySelectorAll('.el-pagination__editor input, .el-pagination .el-input__inner[type="number"]');
      for (var k = 0; k < jumpInputs.length; k++) {
        if (!jumpInputs[k].getAttribute('aria-label')) {
          jumpInputs[k].setAttribute('aria-label', '跳转到指定页');
        }
      }
      // 4) 表格行内 el-switch 开关（裸 checkbox，无 label 包裹）
      var switches = root.querySelectorAll('input[type="checkbox"].el-switch__input, .el-switch input[type="checkbox"], .el-switch input, [role="switch"] input');
      for (var s = 0; s < switches.length; s++) {
        var sw = switches[s];
        if (sw.getAttribute('aria-label')) continue;
        var swRow = sw.closest('tr');
        var swName = '';
        if (swRow) {
          var swCells = swRow.querySelectorAll('td');
          for (var sc = 0; sc < swCells.length; sc++) {
            var st = (swCells[sc].innerText || '').trim();
            if (st) { swName = st.split('\n')[0].slice(0, 20); break; }
          }
        }
        sw.setAttribute('aria-label', swName ? '开关：' + swName : '开关');
      }
      // 5) 其余裸 checkbox/radio（表格行选择等，无 el-checkbox/label 包裹）
      var bareBoxes = root.querySelectorAll('input[type="checkbox"], input[type="radio"]');
      for (var m2 = 0; m2 < bareBoxes.length; m2++) {
        var bb = bareBoxes[m2];
        if (bb.getAttribute('aria-label') || bb.getAttribute('aria-labelledby')) continue;
        if (bb.closest('label') || bb.closest('.el-checkbox') || bb.closest('.el-radio')) continue;
        var inSwitch = bb.closest('.el-switch');
        if (inSwitch) continue; // 已由第 4 步处理
        var bbRow = bb.closest('tr');
        if (bbRow) {
          var bbCells = bbRow.querySelectorAll('td');
          var bbName = '';
          for (var bc = 0; bc < bbCells.length; bc++) {
            var bt = (bbCells[bc].innerText || '').trim();
            if (bt) { bbName = bt.split('\n')[0].slice(0, 20); break; }
          }
          bb.setAttribute('aria-label', bbName ? '选择：' + bbName : '选择此行');
        }
      }
      // 6) 弹窗/表单内无名文本输入：用 el-form-item label 或弹窗标题补名
      var textControls = root.querySelectorAll('input:not([type="hidden"]):not([type="submit"]):not([type="button"]):not([type="checkbox"]):not([type="radio"]), select, textarea');
      for (var t2 = 0; t2 < textControls.length; t2++) {
        var tc = textControls[t2];
        if (tc.getAttribute('aria-label') || tc.getAttribute('aria-labelledby') || tc.getAttribute('title') || tc.getAttribute('placeholder')) continue;
        var fi2 = tc.closest('.el-form-item');
        var lbl = fi2 ? fi2.querySelector('label') : null;
        if (lbl && (lbl.innerText || '').trim()) {
          tc.setAttribute('aria-label', lbl.innerText.trim().slice(0, 30));
          continue;
        }
        var dlg2 = tc.closest('.el-dialog');
        var dlgTitle = dlg2 ? dlg2.querySelector('.el-dialog__title') : null;
        var base = dlgTitle && (dlgTitle.innerText || '').trim() ? dlgTitle.innerText.trim().slice(0, 20) + ' 表单项' : '表单项';
        tc.setAttribute('aria-label', base);
      }
      // 7) 纯图标按钮（Element UI 分页箭头、关闭按钮等）：无可见文字、无 title、无 aria-label
      //    这类按钮在 Element UI 中只渲染 <i class="el-icon-...">，屏幕阅读器读不到任何名字
      var iconBtns = root.querySelectorAll('button');
      for (var n1 = 0; n1 < iconBtns.length; n1++) {
        var ib = iconBtns[n1];
        if (ib.getAttribute('aria-label') || ib.getAttribute('title')) continue;
        if ((ib.textContent || '').trim()) continue;      // 有可见文字则天然有可访问名
        if (ib.disabled || ib.closest('[disabled],.is-disabled')) continue;
        var icls = (ib.className || '').toString();
        var iname = '';
        if (/(^|\s)btn-prev(\s|$)/.test(icls) || /(^|\s)btn-quickprev(\s|$)/.test(icls)) iname = '上一页';
        else if (/(^|\s)btn-next(\s|$)/.test(icls) || /(^|\s)btn-quicknext(\s|$)/.test(icls)) iname = '下一页';
        else if (/(^|\s)btn-close(\s|$)/.test(icls) || /el-dialog__headerbtn/.test(icls)) iname = '关闭';
        else if (ib.closest('.el-pagination')) iname = '分页';
        else if (ib.closest('.el-dropdown')) iname = '更多操作';
        else {
          // el-tooltip 包裹的图标按钮：Element UI 不会把 content 写进可访问名，需从 aria-describedby 取
          var descId = ib.getAttribute('aria-describedby');
          if (descId) {
            var dEl = document.getElementById(descId);
            var dTxt = dEl ? ((dEl.innerText || dEl.textContent || '').trim()) : '';
            if (dTxt) iname = dTxt.slice(0, 24);
          }
        }
        if (!iname) iname = '操作按钮';
        ib.setAttribute('aria-label', iname);
        var ii = ib.querySelector('i');
        if (ii) ii.setAttribute('aria-hidden', 'true');
      }
    } catch (e) { /* 静默失败，绝不影响业务 */ }
  }

  /* ------------------------------------------------------------
   * el-input-number 增减按钮（Element UI 2.x 组件库缺陷）
   * ------------------------------------------------------------
   * 缺陷：组件把增减按钮渲染成
   *     <span class="el-input-number__decrease" role="button"><i class="el-icon-minus"></i></span>
   * 既没有 tabindex（键盘完全无法聚焦，WCAG 2.1.1），内部又只有装饰性图标
   * 没有任何可访问名（WCAG 4.1.2）。全站 30+ 个页面使用，故在共用入口统一补。
   * 注意：该控件在弹窗（el-dialog）里的实例首屏不可见，逐页检查时会被 visible() 过滤，
   * 因此上轮审查只在 sales-report / dish-ranking 零星空穴见报——实际隐患是全站性的。
   * 补丁：补 tabindex="0" 与 aria-label，Enter/空格触发等价 click（span 无原生激活行为）。
   * 幂等：用 data-a11y-num 标记，避免 MutationObserver 反复重绑事件。
   * ------------------------------------------------------------ */
  // 生成 keydown 处理器：闭包捕获传入的 el，而非循环变量
  function makeKeyHandler(el) {
    return function (ev) {
      var k = ev.key;
      if (k === 'Enter' || k === ' ' || k === 'Spacebar') {
        ev.preventDefault();
        // span 没有原生激活行为，必须手动派发等价点击
        el.click();
      }
    };
  }

  function fixInputNumberControls(root) {
    try {
      var nodes = root.querySelectorAll(
        '.el-input-number__decrease, .el-input-number__increase'
      );
      for (var i = 0; i < nodes.length; i++) {
        var n = nodes[i];
        if (n.getAttribute('data-a11y-num')) continue;

        // 禁用态不进入 Tab 序列
        var wrap = n.closest('.el-input-number');
        if (wrap && wrap.classList.contains('is-disabled')) continue;
        if (n.classList.contains('is-disabled')) continue;

        if (n.getAttribute('tabindex') === null) {
          n.setAttribute('tabindex', '0');
        }
        if (!n.getAttribute('aria-label')) {
          // 优先借用 el-form-item 的 label 让读屏念出「数量，减少」而非干巴巴的「减少」
          var field = '';
          var fi = n.closest('.el-form-item');
          var lbl = fi ? fi.querySelector('label') : null;
          if (lbl && (lbl.innerText || '').trim()) {
            field = lbl.innerText.trim().replace(/\s+/g, '').slice(0, 12);
          }
          var act = n.classList.contains('el-input-number__decrease') ? '减少' : '增加';
          n.setAttribute('aria-label', field ? field + ' ' + act : act);
        }
        // 图标纯装饰，避免读屏重复朗读字形名
        var ii2 = n.querySelector('i');
        if (ii2) ii2.setAttribute('aria-hidden', 'true');

        n.setAttribute('data-a11y-num', '1');
        // 注意：必须用工厂函数捕获每个节点自身。
        // 若在循环里写 `var n` + 闭包内引用 n，因 var 是函数作用域、循环复用同一变量，
        // 会导致所有按钮的 Enter 都去点最后一个按钮（本补丁曾踩此坑）。
        n.addEventListener('keydown', makeKeyHandler(n));
      }
    } catch (e2) { /* 静默失败，绝不影响业务 */ }
  }

  function schedule(fn) {
    if (typeof window.requestAnimationFrame === 'function') {
      window.requestAnimationFrame(fn);
    } else { setTimeout(fn, 60); }
  }

  function boot() {
    nameAnonymousControls(document);
    fixInputNumberControls(document);
    // Vue 异步渲染兜底：首屏表格可能晚于 DOMContentLoaded
    setTimeout(function () { nameAnonymousControls(document); }, 800);
    // 弹窗/表格里的 el-input-number 常在数据回填后（>800ms）才出现，补一次扫描
    setTimeout(function () { fixInputNumberControls(document); }, 1500);
    // 翻页/增删行后 DOM 重建：MutationObserver 兜底（去抖）
    var pending = false;
    if (typeof MutationObserver === 'function' && document.body) {
      new MutationObserver(function () {
        if (pending) return;
        pending = true;
        schedule(function () {
          pending = false;
          nameAnonymousControls(document);
          // 弹窗/折叠面板内的 el-input-number 是后渲染的，必须一并补
          fixInputNumberControls(document);
        });
      }).observe(document.body, {
        childList: true,
        subtree: true,
        // 关键：el-input-number 的 disabled 是动态切换的（值到 min 时减号禁用、加回来后启用），
        // 只观察 childList 的话，按钮由禁用转为可用后 class 变化不会触发扫描，
        // 该按钮就永远补不上 tabindex。故一并观察 class 变化。
        attributes: true,
        attributeFilter: ['class'],
      });
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
