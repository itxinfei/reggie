/**
 * AI 聊天公共内核（管理后台 + C 端共用，零框架依赖，ES5 语法）
 *
 * 提供三个模块，挂在 window.AiChatCore：
 * 1. ChatClient     —— POST fetch + ReadableStream 消费 SSE（\n\n 分帧），
 *                     AbortController 停止生成，CSRF 头自动携带，非流式兜底
 * 2. Markdown       —— 安全 Markdown 渲染（先 HTML 转义再白名单替换，零第三方依赖）
 * 3. MessageFactory —— 消息模型工厂（本地临时 key + done 回填真实 messageId）
 *
 * SSE 事件协议（与后端 ChatStreamSession 对齐）：
 *   capabilities {chat, vision, tools}
 *   message      {text}
 *   dishes       [{id,name,price,description,...}]（JSON 字符串，点餐场景）
 *   done         {status, stopped, conversationId, messageId}
 *   error        {message}
 *
 * @author reggie
 * @since 2026-09-20
 */
(function (window) {
    'use strict';

    var AiChatCore = {};

    /* ============================== 工具函数 ============================== */

    /**
     * 读取 Cookie 值
     */
    function getCookie(name) {
        var cookies = document.cookie ? document.cookie.split(';') : [];
        for (var i = 0; i < cookies.length; i++) {
            var pair = cookies[i].trim();
            if (pair.indexOf(name + '=') === 0) {
                return decodeURIComponent(pair.substring(name.length + 1));
            }
        }
        return null;
    }

    /**
     * 获取 CSRF Token：优先 Cookie，其次 sessionStorage（与 js/request.js 约定一致）
     */
    function getCsrfToken() {
        var token = getCookie('csrfToken');
        if (token) return token;
        try {
            return window.sessionStorage.getItem('csrfToken');
        } catch (e) {
            return null;
        }
    }

    /**
     * 生成前端临时 ID（用于消息 key、clientMsgId 幂等键）
     */
    function genId(prefix) {
        return (prefix || 'id') + '-' + Date.now().toString(36)
            + '-' + Math.random().toString(36).substring(2, 8);
    }

    /* ============================== 安全 Markdown ============================== */

    var Markdown = (function () {

        var ENTITY_MAP = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        };

        /** HTML 实体转义（安全渲染第一步，所有文本必经此函数） */
        function escapeHtml(text) {
            return String(text == null ? '' : text).replace(/[&<>"']/g, function (ch) {
                return ENTITY_MAP[ch];
            });
        }

        var PLACEHOLDER = {
            CODE: '\u0000C',
            LINK: '\u0000L',
            IMAGE: '\u0000I'
        };

        function restorePh(text, store, marker) {
            var re = new RegExp(marker + '(\\d+)', 'g');
            return text.replace(re, function (_, idx) {
                return store[parseInt(idx, 10)] || '';
            });
        }

        /**
         * 行内格式化（入参已做 HTML 转义）：
         * 行内代码/图片/链接先占位提取 → 粗体/删除线/斜体 → 裸链接自动补链 → 回填占位
         */
        function inlineFormat(src) {
            var text = src;
            var codeStore = [];
            var linkStore = [];
            var imageStore = [];

            // 行内代码 `code`（内容不再做任何 markdown 解析）
            text = text.replace(/`([^`\n]+)`/g, function (_, code) {
                codeStore.push('<code class="md-code-inline">' + code + '</code>');
                return PLACEHOLDER.CODE + (codeStore.length - 1);
            });

            // 图片 ![alt](https?://url)
            text = text.replace(/!\[([^\]\n]*)\]\((https?:\/\/[^\s)]+)\)/g, function (_, alt, url) {
                imageStore.push('<img class="md-img" src="' + url + '" alt="' + alt + '" loading="lazy">');
                return PLACEHOLDER.IMAGE + (imageStore.length - 1);
            });

            // 链接 [text](https?://url)，强制新标签 + noopener
            text = text.replace(/\[([^\]\n]+)\]\((https?:\/\/[^\s)]+)\)/g, function (_, label, url) {
                linkStore.push('<a href="' + url + '" target="_blank" rel="noopener noreferrer">' + label + '</a>');
                return PLACEHOLDER.LINK + (linkStore.length - 1);
            });

            // 粗体 **x** / __x__
            text = text.replace(/\*\*([^*]+?)\*\*/g, '<strong class="md-strong">$1</strong>');
            text = text.replace(/__([^_]+?)__/g, '<strong class="md-strong">$1</strong>');
            // 删除线 ~~x~~
            text = text.replace(/~~([^~]+?)~~/g, '<del class="md-del">$1</del>');
            // 斜体 *x*（排除 ** 已处理场景）
            text = text.replace(/(^|[^*])\*([^*\n]+?)\*(?!\*)/g, '$1<em class="md-em">$2</em>');

            // 裸 URL 自动补链（占位符内的链接不受影响）
            text = text.replace(/(https?:\/\/[^\s<\])）"']+)/g, function (url) {
                var tail = '';
                while (/[.,;:!?。，；：！？）)]$/.test(url) && url.length > 1) {
                    tail = url.charAt(url.length - 1) + tail;
                    url = url.substring(0, url.length - 1);
                }
                return '<a href="' + url + '" target="_blank" rel="noopener noreferrer">' + url + '</a>' + tail;
            });

            text = restorePh(text, imageStore, PLACEHOLDER.IMAGE);
            text = restorePh(text, linkStore, PLACEHOLDER.LINK);
            text = restorePh(text, codeStore, PLACEHOLDER.CODE);
            return text;
        }

        function isBlank(line) {
            return /^\s*$/.test(line);
        }

        /** 拆分表格行为单元格数组 */
        function splitTableRow(rowLine) {
            var line = rowLine.trim();
            if (line.charAt(0) === '|') line = line.substring(1);
            if (line.charAt(line.length - 1) === '|') line = line.substring(0, line.length - 1);
            var cells = line.split('|');
            for (var i = 0; i < cells.length; i++) {
                cells[i] = cells[i].trim();
            }
            return cells;
        }

        /**
         * Markdown → 安全 HTML
         * 支持：围栏代码块、表格、标题、引用、有序/无序列表、分割线、
         *       段落+换行、行内代码/粗体/斜体/删除线/链接/图片/裸链接
         */
        function render(src) {
            if (!src) return '';
            var text = escapeHtml(src).replace(/\r\n?/g, '\n');

            // 1) 提取围栏代码块（内容受保护，不做后续解析）
            var blockStore = [];
            text = text.replace(/```([^\n`]*)\n?([\s\S]*?)```/g, function (_, lang, code) {
                var langAttr = '';
                if (lang && /^[A-Za-z0-9_+\-.]{0,20}$/.test(lang.trim())) {
                    langAttr = ' data-lang="' + lang.trim() + '"';
                }
                var body = code.replace(/\n$/, '');
                blockStore.push('<pre class="md-code-block"' + langAttr + '><code>' + body + '</code></pre>');
                return PLACEHOLDER.CODE + (blockStore.length - 1);
            });

            var lines = text.split('\n');
            var html = [];
            var i = 0;

            while (i < lines.length) {
                var line = lines[i];

                // 代码块占位独占一行
                var blockMatch = line.match(new RegExp('^' + PLACEHOLDER.CODE + '(\\d+)$'));
                if (blockMatch) {
                    html.push(blockStore[parseInt(blockMatch[1], 10)]);
                    i++;
                    continue;
                }

                // 空行
                if (isBlank(line)) {
                    i++;
                    continue;
                }

                // 表格：当前行含 |，下一行是对齐分隔行（至少含一个 -，首尾竖线允许省略）
                if (i + 1 < lines.length
                        && /^\s*\|?[\s:|-]*-[\s:|-]*$/.test(lines[i + 1])
                        && lines[i + 1].indexOf('|') > -1
                        && line.indexOf('|') > -1) {
                    var headerCells = splitTableRow(line);
                    var alignCells = splitTableRow(lines[i + 1]);
                    var aligns = [];
                    for (var a = 0; a < headerCells.length; a++) {
                        var sep = (alignCells[a] || '').trim();
                        var colonLeft = sep.charAt(0) === ':';
                        var colonRight = sep.charAt(sep.length - 1) === ':';
                        aligns.push(colonLeft && colonRight ? 'center' : (colonRight ? 'right' : 'left'));
                    }
                    var tableHtml = ['<div class="md-table-wrap"><table class="md-table"><thead><tr>'];
                    for (var h = 0; h < headerCells.length; h++) {
                        tableHtml.push('<th style="text-align:' + aligns[h] + '">'
                            + inlineFormat(headerCells[h]) + '</th>');
                    }
                    tableHtml.push('</tr></thead><tbody>');
                    var r = i + 2;
                    while (r < lines.length && !isBlank(lines[r]) && lines[r].indexOf('|') > -1) {
                        var rowCells = splitTableRow(lines[r]);
                        tableHtml.push('<tr>');
                        for (var c = 0; c < headerCells.length; c++) {
                            tableHtml.push('<td style="text-align:' + aligns[c] + '">'
                                + inlineFormat(rowCells[c] != null ? rowCells[c] : '') + '</td>');
                        }
                        tableHtml.push('</tr>');
                        r++;
                    }
                    tableHtml.push('</tbody></table></div>');
                    html.push(tableHtml.join(''));
                    i = r;
                    continue;
                }

                // 标题 ######
                var headingMatch = line.match(/^(#{1,6})\s+(.*)$/);
                if (headingMatch) {
                    var level = headingMatch[1].length;
                    html.push('<h' + level + ' class="md-h md-h-' + level + '">'
                        + inlineFormat(headingMatch[2]) + '</h' + level + '>');
                    i++;
                    continue;
                }

                // 引用 >（连续多行合并）
                if (/^&gt;/.test(line.trim())) {
                    var quoteLines = [];
                    while (i < lines.length && /^&gt;/.test(lines[i].trim())) {
                        quoteLines.push(lines[i].trim().replace(/^&gt;\s?/, ''));
                        i++;
                    }
                    html.push('<blockquote class="md-quote">'
                        + inlineFormat(quoteLines.join('<br>')) + '</blockquote>');
                    continue;
                }

                // 无序列表 - / * / ·
                if (/^\s*[-*·]\s+/.test(line)) {
                    var ulItems = [];
                    while (i < lines.length && /^\s*[-*·]\s+/.test(lines[i])) {
                        ulItems.push('<li>' + inlineFormat(lines[i].replace(/^\s*[-*·]\s+/, '')) + '</li>');
                        i++;
                    }
                    html.push('<ul class="md-ul">' + ulItems.join('') + '</ul>');
                    continue;
                }

                // 有序列表 1. / 1)
                if (/^\s*\d+[.)]\s+/.test(line)) {
                    var olItems = [];
                    while (i < lines.length && /^\s*\d+[.)]\s+/.test(lines[i])) {
                        olItems.push('<li>' + inlineFormat(lines[i].replace(/^\s*\d+[.)]\s+/, '')) + '</li>');
                        i++;
                    }
                    html.push('<ol class="md-ol">' + olItems.join('') + '</ol>');
                    continue;
                }

                // 分割线
                if (/^\s*([-*_])\s*(?:\1\s*){2,}$/.test(line)) {
                    html.push('<hr class="md-hr">');
                    i++;
                    continue;
                }

                // 普通段落：聚合到空行/块级元素为止
                var paraLines = [];
                while (i < lines.length
                        && !isBlank(lines[i])
                        && !new RegExp('^' + PLACEHOLDER.CODE + '\\d+$').test(lines[i])
                        && !/^(#{1,6})\s+/.test(lines[i])
                        && !/^&gt;/.test(lines[i].trim())
                        && !/^\s*[-*·]\s+/.test(lines[i])
                        && !/^\s*\d+[.)]\s+/.test(lines[i])) {
                    paraLines.push(lines[i]);
                    i++;
                }
                html.push('<p class="md-p">' + inlineFormat(paraLines.join('<br>')) + '</p>');
            }

            var result = html.join('\n');
            // 回填可能落在段落文本中的代码块占位（围栏未闭合时不出现，正常情况下独占行已处理）
            result = restorePh(result, blockStore, PLACEHOLDER.CODE);
            return result;
        }

        return {
            render: render,
            escapeHtml: escapeHtml,
            inline: inlineFormat
        };
    })();

    /* ============================== SSE 流式客户端 ============================== */

    /**
     * 读取非 2xx 响应的错误信息
     */
    function readErrorResponse(res) {
        return res.text().then(function (text) {
            var message = '请求失败（HTTP ' + res.status + '）';
            var notLogin = false;
            try {
                var body = JSON.parse(text);
                if (body && (body.msg || body.message)) {
                    message = body.msg || body.message;
                }
                if (body && body.code === 0 && body.msg === 'NOTLOGIN') {
                    notLogin = true;
                    message = '登录已过期，请重新登录';
                }
            } catch (e) { /* 非 JSON 错误体，保留默认文案 */ }
            if (res.status === 401) notLogin = true;
            return { message: message, status: res.status, notLogin: notLogin };
        }).catch(function () {
            return { message: '请求失败（HTTP ' + res.status + '）', status: res.status, notLogin: res.status === 401 };
        });
    }

    /**
     * 解析单个 SSE 帧并分发给回调
     * 帧格式：event:xxx \n data:{...}（event 缺省视为 message）
     */
    function dispatchSseFrame(rawFrame, handlers) {
        if (!rawFrame || !rawFrame.trim()) return;
        var eventName = 'message';
        var dataLines = [];
        var frameLines = rawFrame.split(/\r?\n/);
        for (var i = 0; i < frameLines.length; i++) {
            var fl = frameLines[i];
            if (fl.indexOf('event:') === 0) {
                eventName = fl.substring(6).trim();
            } else if (fl.indexOf('data:') === 0) {
                dataLines.push(fl.substring(5).replace(/^ /, ''));
            }
            // id:/retry:/注释行（:开头）忽略
        }
        var dataStr = dataLines.join('\n');
        var data;
        try {
            data = dataStr ? JSON.parse(dataStr) : {};
        } catch (e) {
            data = { raw: dataStr };
        }
        if (eventName === 'capabilities' && handlers.onCapabilities) {
            handlers.onCapabilities(data);
        } else if (eventName === 'message' && handlers.onToken) {
            handlers.onToken(data.text || '', data);
        } else if (eventName === 'dishes' && handlers.onDishes) {
            // 后端 dishes 事件 data 为 JSON 字符串
            var dishes = data;
            if (typeof data.raw === 'string') {
                try { dishes = JSON.parse(data.raw); } catch (e2) { dishes = []; }
            }
            handlers.onDishes(dishes || []);
        } else if (eventName === 'done' && handlers.onDone) {
            handlers.onDone(data);
        } else if (eventName === 'error') {
            var errorHandler = handlers.onProtocolError || handlers.onError;
            if (errorHandler) {
                errorHandler(data.message || '服务暂时不可用，请稍后重试', data);
            }
        }
    }

    /**
     * SSE 流式客户端
     * 用法：
     *   var client = new AiChatCore.ChatClient();
     *   client.stream({ url, body, onToken, onDone, onError, onAbort, ... });
     *   client.stop();  // 用户点「停止生成」
     */
    function ChatClient() {
        this.controller = null;
        this.finished = false;
    }

    ChatClient.prototype._buildHeaders = function () {
        var headers = {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream',
            'X-Requested-With': 'XMLHttpRequest'
        };
        var token = getCsrfToken();
        if (token) {
            headers['X-CSRF-Token'] = token;
        }
        return headers;
    };

    /**
     * 发起流式请求
     * @param {Object} cfg url/body + 回调（onCapabilities/onToken/onDishes/onDone/onError/onAbort）
     */
    ChatClient.prototype.stream = function (cfg) {
        var self = this;
        // 单次实例不并发：先终止旧请求
        self.stop();
        self.finished = false;

        var controller = null;
        if (typeof window.AbortController !== 'undefined') {
            controller = new window.AbortController();
        }
        self.controller = controller;

        var userAborted = false;

        function finalize(handler) {
            if (self.finished) return;
            self.finished = true;
            self.controller = null;
            try { handler(); } catch (e) { /* 回调异常不影响状态机 */ }
        }

        var fetchOptions = {
            method: 'POST',
            headers: self._buildHeaders(),
            body: JSON.stringify(cfg.body || {}),
            credentials: 'same-origin'
        };
        if (controller) fetchOptions.signal = controller.signal;

        window.fetch(cfg.url, fetchOptions).then(function (res) {
            if (!res.ok) {
                readErrorResponse(res).then(function (info) {
                    finalize(function () {
                        if (cfg.onError) cfg.onError(info.message, info);
                    });
                });
                return;
            }

            // 老浏览器无 ReadableStream：读全文后一次性按帧回放
            if (!res.body || typeof res.body.getReader !== 'function') {
                res.text().then(function (fullText) {
                    var frames = fullText.split('\n\n');
                    for (var i = 0; i < frames.length; i++) {
                        dispatchSseFrame(frames[i], cfg);
                    }
                    finalize(function () {
                        if (cfg.onComplete) cfg.onComplete();
                    });
                });
                return;
            }

            var reader = res.body.getReader();
            var decoder = new window.TextDecoder('utf-8');
            var buffer = '';

            function pump() {
                reader.read().then(function (chunk) {
                    if (chunk.done) {
                        if (buffer.trim()) dispatchSseFrame(buffer, cfg);
                        finalize(function () {
                            if (cfg.onComplete) cfg.onComplete();
                        });
                        return;
                    }
                    buffer += decoder.decode(chunk.value, { stream: true });
                    var frames = buffer.split('\n\n');
                    // 最后一段可能是不完整帧，留在 buffer
                    buffer = frames.pop();
                    for (var i = 0; i < frames.length; i++) {
                        dispatchSseFrame(frames[i], cfg);
                    }
                    pump();
                }).catch(function (err) {
                    if (userAborted || (controller && controller.signal.aborted)) {
                        finalize(function () {
                            if (cfg.onAbort) cfg.onAbort();
                        });
                        return;
                    }
                    finalize(function () {
                        if (cfg.onError) cfg.onError('网络连接中断，请重试', { network: true });
                    });
                });
            }
            pump();
        }).catch(function (err) {
            if (userAborted || (controller && controller.signal.aborted)) {
                finalize(function () {
                    if (cfg.onAbort) cfg.onAbort();
                });
                return;
            }
            finalize(function () {
                if (cfg.onError) cfg.onError('网络异常，请稍后重试', { network: true });
            });
        });

        // 标记中止来源：仅用户主动 stop 触发 onAbort
        self._onUserStop = function () { userAborted = true; };
    };

    /** 用户主动停止生成（服务端会把已生成片段以 stopped 状态落库） */
    ChatClient.prototype.stop = function () {
        if (this._onUserStop) {
            try { this._onUserStop(); } catch (e) { /* noop */ }
            this._onUserStop = null;
        }
        if (this.controller) {
            try { this.controller.abort(); } catch (e) { /* noop */ }
        }
    };

    ChatClient.prototype.isActive = function () {
        return !this.finished;
    };

    /* ============================== 非流式 POST 兜底 ============================== */

    /**
     * 普通 JSON POST（非流式降级使用，携带同样的 CSRF/鉴权头）
     * @return Promise resolve 响应 JSON
     */
    function postJSON(url, body) {
        var headers = {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        };
        var token = getCsrfToken();
        if (token) headers['X-CSRF-Token'] = token;
        return window.fetch(url, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(body || {}),
            credentials: 'same-origin'
        }).then(function (res) {
            return res.text().then(function (text) {
                var parsed = null;
                try { parsed = text ? JSON.parse(text) : null; } catch (e) { parsed = null; }
                return { ok: res.ok, status: res.status, data: parsed, raw: text };
            });
        });
    }

    /* ============================== 消息工厂 ============================== */

    var MessageFactory = {

        /** 构造本地用户消息（携带幂等 clientMsgId） */
        createUserMessage: function (text, options) {
            var opts = options || {};
            return {
                key: genId('u'),
                clientMsgId: opts.clientMsgId || genId('c'),
                role: 'user',
                content: text,
                streaming: false,
                stopped: false,
                error: false,
                attachments: opts.attachments || null,
                dbMessageId: null,
                feedback: null,
                timestamp: Date.now()
            };
        },

        /** 构造流式 AI 占位消息 */
        createAssistantMessage: function () {
            return {
                key: genId('a'),
                role: 'assistant',
                content: '',
                streaming: true,
                stopped: false,
                error: false,
                attachments: null,
                dbMessageId: null,
                feedback: null,
                dishes: null,
                timestamp: Date.now()
            };
        },

        /** 从后端历史记录构造消息（status: completed/stopped/failed） */
        fromRecord: function (record) {
            var attachments = null;
            if (record.attachments) {
                try {
                    attachments = typeof record.attachments === 'string'
                        ? JSON.parse(record.attachments) : record.attachments;
                } catch (e) {
                    attachments = null;
                }
            }
            return {
                key: 'hist-' + record.id,
                dbMessageId: record.id,
                role: record.role || 'user',
                content: record.content || '',
                streaming: false,
                stopped: record.status === 'stopped',
                error: record.status === 'failed',
                attachments: attachments,
                feedback: record.feedback || null,
                dishes: null,
                timestamp: record.createTime ? new Date(record.createTime).getTime() : Date.now()
            };
        },

        genClientMsgId: function () {
            return genId('c');
        }
    };

    /* ============================== 剪贴板 ============================== */

    /**
     * 复制文本：优先 Clipboard API，降级 execCommand
     * @return Promise
     */
    function copyText(text) {
        if (window.navigator && window.navigator.clipboard && window.navigator.clipboard.writeText) {
            return window.navigator.clipboard.writeText(text);
        }
        return new Promise(function (resolve, reject) {
            var textarea = document.createElement('textarea');
            textarea.value = text;
            textarea.setAttribute('readonly', 'readonly');
            textarea.style.position = 'fixed';
            textarea.style.opacity = '0';
            document.body.appendChild(textarea);
            textarea.select();
            var ok = false;
            try { ok = document.execCommand('copy'); } catch (e) { ok = false; }
            document.body.removeChild(textarea);
            if (ok) resolve(); else reject(new Error('copy failed'));
        });
    }

    /* ============================== 导出 ============================== */

    AiChatCore.ChatClient = ChatClient;
    AiChatCore.Markdown = Markdown;
    AiChatCore.MessageFactory = MessageFactory;
    AiChatCore.copyText = copyText;
    AiChatCore.getCsrfToken = getCsrfToken;
    AiChatCore.genId = genId;
    AiChatCore.postJSON = postJSON;

    window.AiChatCore = AiChatCore;
})(window);
