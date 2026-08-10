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
