/**
 * 浏览器小票打印工具
 *
 * 关键限制（务必知晓）：
 * 1. window.print() 调本地系统打印机时，浏览器必然弹出系统打印对话框、需员工
 *    手动点「打印」，网页无法静默自动出纸（除非以 --kiosk-printing 启动浏览器）。
 * 2. 网页无法获知员工最终点了「打印」还是「取消」。因此打印记录（谁/何时/
 *    何单）由后端在「发起打印」时即落库，与本工具的最终结果无关。
 * 3. 用隐藏 iframe 承载等宽小票文本，只打印小票、不带走整页后台界面。
 */
(function (global) {
  'use strict'

  // 58mm 热敏纸可打印宽约 48mm：等宽字体、零边距，11px 下一行约容纳
  // 16 个全角字（=32 半角宽），与后端小票排版宽度一致。
  function buildHtml(text, title) {
    var safe = String(text == null ? '' : text)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
    return '<!doctype html><html><head><meta charset="utf-8">' +
      '<title>' + (title || '打印小票') + '</title>' +
      '<style>' +
      '@page{margin:0}' +
      'html,body{margin:0;padding:0;background:#fff}' +
      'body{font-family:Consolas,"Courier New","SimSun",monospace}' +
      'pre{margin:0;padding:2mm;white-space:pre-wrap;font-size:11px;' +
      'line-height:1.25;color:#000}' +
      '</style></head><body><pre>' + safe + '</pre></body></html>'
  }

  /**
   * 在隐藏 iframe 中打印等宽文本。
   * @param {string} text  小票纯文本
   * @param {string} title 打印任务/文档标题
   */
  function printText(text, title) {
    var iframe = document.createElement('iframe')
    iframe.setAttribute('aria-hidden', 'true')
    // 用 0 尺寸而非 display:none：display:none 的 iframe 在部分浏览器无法被打印。
    iframe.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0;'
    document.body.appendChild(iframe)

    var cleaned = false
    function cleanup() {
      if (cleaned) return
      cleaned = true
      if (iframe.parentNode) iframe.parentNode.removeChild(iframe)
    }

    var win = iframe.contentWindow
    // 用户在系统对话框点「打印」或「取消」都会触发 afterprint，届时回收 iframe。
    win.addEventListener('afterprint', cleanup)
    // 兜底：员工长时间不处理对话框（2 分钟）也回收，避免残留节点。
    global.setTimeout(cleanup, 120000)

    iframe.onload = function () {
      try {
        win.focus()
        win.print()
      } catch (e) {
        cleanup()
        if (global.ReggieUI && global.ReggieUI.error) {
          global.ReggieUI.error('调起打印失败，请改用浏览器打印')
        }
      }
    }

    var doc = win.document
    doc.open()
    doc.write(buildHtml(text, title))
    doc.close()
  }

  global.PrintUtil = { printText: printText }
})(window)
