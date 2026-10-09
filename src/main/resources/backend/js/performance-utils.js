/**
 * 性能优化工具函数
 * 提供防抖、节流、懒加载等功能
 */
const PerformanceUtils = {
  /**
   * 防抖函数
   * @param {Function} func - 要防抖的函数
   * @param {number} wait - 等待时间（毫秒）
   * @param {boolean} immediate - 是否立即执行
   * @returns {Function} 防抖后的函数
   */
  debounce(func, wait = 300, immediate = false) {
    let timeout;
    return function executedFunction(...args) {
      const context = this;
      const later = function() {
        timeout = null;
        if (!immediate) func.apply(context, args);
      };
      const callNow = immediate && !timeout;
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
      if (callNow) func.apply(context, args);
    };
  },
  
  /**
   * 节流函数
   * @param {Function} func - 要节流的函数
   * @param {number} limit - 限制时间（毫秒）
   * @returns {Function} 节流后的函数
   */
  throttle(func, limit = 300) {
    let inThrottle;
    return function executedFunction(...args) {
      const context = this;
      if (!inThrottle) {
        func.apply(context, args);
        inThrottle = true;
        setTimeout(() => inThrottle = false, limit);
      }
    };
  },
  
  /**
   * 图片懒加载
   * @param {string} selector - 图片选择器
   * @param {Object} options - 配置选项
   */
  lazyLoadImages(selector = 'img[data-src]', options = {}) {
    const defaultOptions = {
      root: null,
      rootMargin: '0px',
      threshold: 0.1
    };
    
    const mergedOptions = { ...defaultOptions, ...options };
    
    const observer = new IntersectionObserver((entries, observer) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const img = entry.target;
          img.src = img.dataset.src;
          img.removeAttribute('data-src');
          observer.unobserve(img);
        }
      });
    }, mergedOptions);
    
    document.querySelectorAll(selector).forEach(img => {
      observer.observe(img);
    });
  },
  
  /**
   * 虚拟列表（用于大数据量表格）
   * @param {HTMLElement} container - 容器元素
   * @param {Array} items - 数据列表
   * @param {Function} renderItem - 渲染函数
   * @param {number} itemHeight - 每项高度
   */
  virtualList(container, items, renderItem, itemHeight = 40) {
    if (!container) return;

    const containerHeight = container.clientHeight;
    const visibleCount = Math.ceil(containerHeight / itemHeight);
    const totalCount = items.length;

    // 使用独立的内容层，避免清空 container.innerHTML（会破坏监听器和子节点）
    let content = container.querySelector('.rg-vl-content');
    if (!content) {
      content = document.createElement('div');
      content.className = 'rg-vl-content';
      content.style.position = 'relative';
      content.style.height = (totalCount * itemHeight) + 'px';
      container.style.position = container.style.position || 'relative';
      container.appendChild(content);
    } else {
      content.style.height = (totalCount * itemHeight) + 'px';
    }

    const renderVisibleItems = () => {
      const scrollTop = container.scrollTop;
      const startIndex = Math.max(0, Math.floor(scrollTop / itemHeight));
      const endIndex = Math.min(startIndex + visibleCount + 1, totalCount);

      // 只清空内容层，不破坏容器本身
      while (content.firstChild) {
        content.removeChild(content.firstChild);
      }
      for (let i = startIndex; i < endIndex; i++) {
        const element = renderItem(items[i], i);
        element.style.position = 'absolute';
        element.style.top = (i * itemHeight) + 'px';
        element.style.width = '100%';
        content.appendChild(element);
      }
    };

    const handler = this.throttle(renderVisibleItems, 16);
    container.addEventListener('scroll', handler, { passive: true });
    // 暴露清理方法，供组件销毁时移除监听器（防止标签切换时内存泄漏）
    container._rgVlCleanup = () => container.removeEventListener('scroll', handler);
    renderVisibleItems();
  },
  
  /**
   * 预加载资源
   * @param {Array} resources - 资源列表
   * @returns {Promise} 加载完成的Promise
   */
  preloadResources(resources) {
    const promises = resources.map(resource => {
      return new Promise((resolve, reject) => {
        if (resource.type === 'image') {
          const img = new Image();
          img.onload = resolve;
          img.onerror = reject;
          img.src = resource.url;
        } else if (resource.type === 'script') {
          const script = document.createElement('script');
          script.onload = resolve;
          script.onerror = reject;
          script.src = resource.url;
          document.head.appendChild(script);
        } else if (resource.type === 'style') {
          const link = document.createElement('link');
          link.rel = 'stylesheet';
          link.onload = resolve;
          link.onerror = reject;
          link.href = resource.url;
          document.head.appendChild(link);
        }
      });
    });
    
    return Promise.all(promises);
  },
  
  /**
   * 缓存管理
   */
  cache: {
    /**
     * 设置缓存
     * @param {string} key - 缓存键
     * @param {*} value - 缓存值
     * @param {number} ttl - 过期时间（秒）
     */
    set(key, value, ttl = 3600) {
      const item = {
        value: value,
        expiry: Date.now() + ttl * 1000
      };
      localStorage.setItem(key, JSON.stringify(item));
    },
    
    /**
     * 获取缓存
     * @param {string} key - 缓存键
     * @returns {*} 缓存值
     */
    get(key) {
      const itemStr = localStorage.getItem(key);
      if (!itemStr) return null;
      
      const item = JSON.parse(itemStr);
      if (Date.now() > item.expiry) {
        localStorage.removeItem(key);
        return null;
      }
      
      return item.value;
    },
    
    /**
     * 删除缓存
     * @param {string} key - 缓存键
     */
    remove(key) {
      localStorage.removeItem(key);
    },
    
    /**
     * 清空缓存
     */
    clear() {
      localStorage.clear();
    }
  }
};

// 导出工具函数
if (typeof module !== 'undefined' && module.exports) {
  module.exports = PerformanceUtils;
}
