/**
 * Emoji 图标到 RemixIcon 的映射工具
 * 用于统计卡片等场景，将不专业的 emoji 替换为统一的图标字体
 *
 * 使用方式：
 *   <i :class="EmojiToIcon.toClass('🍽️')"></i>
 *   EmojiToIcon.toClass('🍽️') → 'ri-restaurant-line'
 */
window.EmojiToIcon = {
  // 食物 / 餐饮
  '🍽️': 'ri-restaurant-line',
  '🍱': 'ri-box-3-line',
  '🍔': 'ri-restaurant-2-line',
  '🍕': 'ri-restaurant-2-line',
  '🥗': 'ri-leaf-line',
  '☕': 'ri-cup-line',
  '🍰': 'ri-cake-3-line',
  '🍷': 'ri-goblet-line',

  // 文档 / 数据
  '📊': 'ri-bar-chart-box-line',
  '📈': 'ri-line-chart-line',
  '📉': 'ri-line-chart-line',
  '📋': 'ri-file-list-line',
  '📂': 'ri-folders-line',
  '📁': 'ri-folder-line',
  '📄': 'ri-file-line',
  '📑': 'ri-bookmark-line',

  // 状态 / 标识
  '✅': 'ri-checkbox-circle-line',
  '⏸️': 'ri-pause-line',
  '⏹️': 'ri-stop-line',
  '▶️': 'ri-play-line',
  '🆕': 'ri-add-circle-line',
  '🗑️': 'ri-delete-bin-line',
  '🔌': 'ri-plug-line',
  '🖨️': 'ri-printer-line',

  // 等级 / 奖章
  '🏅': 'ri-medal-line',
  '👑': 'ri-vip-crown-line',
  '🎯': 'ri-focus-3-line',
  '💯': 'ri-percent-line',
  '🏆': 'ri-trophy-line',
  '⭐': 'ri-star-line',

  // 时钟 / 时间
  '🕐': 'ri-time-line',
  '⏰': 'ri-alarm-line',
  '📅': 'ri-calendar-line',

  // 钱 / 交易
  '💰': 'ri-money-cny-circle-line',
  '💵': 'ri-money-dollar-circle-line',
  '💳': 'ri-bank-card-line',

  // 人员 / 用户
  '👤': 'ri-user-line',
  '👥': 'ri-team-line',

  // 通用
  '🚘': 'ri-truck-line',
  '🏠': 'ri-home-line',
  '⚙️': 'ri-settings-3-line',
  '🔧': 'ri-tools-line',
  '🔍': 'ri-search-line',
  'ℹ️': 'ri-information-line',
  '⚠️': 'ri-error-warning-line',
  '❌': 'ri-close-circle-line',
  '✔️': 'ri-check-line',
  '🎉': 'ri-gift-line',

  /**
   * 将 emoji 转换为 RemixIcon class
   * @param {string} emoji emoji 字符
   * @returns {string} RemixIcon class
   */
  toClass: function(emoji) {
    return this[emoji] || 'ri-question-line';
  }
};
