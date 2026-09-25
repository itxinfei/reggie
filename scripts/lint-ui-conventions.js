#!/usr/bin/env node
/**
 * UI 规范检测脚本 — 方案1：ESLint + Stylelint 强制规范
 * ============================================================
 * 扫描 backend/page/*.html，检测偏离设计系统的写法，
 * 阻止新增违规代码进入仓库。
 *
 * 用法：
 *   node scripts/lint-ui-conventions.js [--fix] [--quiet]
 *
 * 选项：
 *   --fix    输出修复建议（不自动修改文件）
 *   --quiet  只输出 ERROR 级别，隐藏 WARNING
 *
 * 检测规则：
 *   E001  el-button type="danger" 应改用 class="btn-delete"（操作列语义类名）
 *   E002  this.$confirm / this.$message 应改用 ReggieUI.confirm / ReggieUI.success
 *   E003  原生 el-dialog 应改用 crud-dialog 组件
 *   W001  手写 stat-card/stat-row 应改用 stat-cards 组件
 *   W002  手写 filter-bar/cs-filter 应改用 table-bar 组件
 *   W003  label-width 非 100px（表单标签宽度不统一）
 *   W004  硬编码十六进制色值（应使用 CSS 变量/设计令牌）
 *   W005  缺少 ds-page 类（页面内边距不统一）
 *   W006  aria-label="页面主要内容" 应改用 aria-labelledby="page-title"
 *
 * @author Reggie Team
 * @since 2026-09-22
 */

const fs = require('fs');
const path = require('path');

// ── 配置 ──────────────────────────────────────────────
const PAGE_DIR = path.resolve(__dirname, '..', 'src', 'main', 'resources', 'backend', 'page');
const STYLE_DIR = path.resolve(__dirname, '..', 'src', 'main', 'resources', 'backend', 'styles');

const args = process.argv.slice(2);
const showFix = args.includes('--fix');
const quiet = args.includes('--quiet');

// ── 规则定义 ──────────────────────────────────────────
const rules = {
  E001: {
    level: 'ERROR',
    desc: 'el-button type="danger" 应改用 class="btn-delete"',
    pattern: /<el-button[^>]*\btype="danger"[^>]*>/g,
    exclude: /class="btn-delete"/,  // 已使用语义类名的例外
    fix: '操作列删除按钮改用 <el-button type="text" size="small" class="btn-delete">删除</el-button>；独立危险操作区可保留 type="danger"'
  },
  E002: {
    level: 'ERROR',
    desc: 'this.$confirm / this.$message 应改用 ReggieUI',
    pattern: /this\.\$(confirm|message|notify|alert)\s*\(/g,
    fix: '使用 ReggieUI.confirm() / ReggieUI.success() / ReggieUI.warning() / ReggieUI.error()'
  },
  E003: {
    level: 'ERROR',
    desc: '原生 el-dialog 应改用 crud-dialog 组件',
    pattern: /<el-dialog\b/g,
    // 排除 crud-dialog 内部模板中的 el-dialog（components.js 中的实现）
    excludeFile: /components\.js$/,
        fix: '使用 <crud-dialog :visible.sync="xxx" title="标题" size="md"> 替代，size 支持 sm/md/lg/xl/fullscreen；自定义头部用 #header 插槽'
  },
  W001: {
    level: 'WARNING',
    desc: '手写 stat-card/stat-row 应改用 stat-cards 组件',
    // 只匹配容器级 class（stat-row / 独立 stat-card），排除 BEM 子类如 stat-card__label/__value
    pattern: /\bclass="[^"]*\bstat-row\b[^"]*"|\bclass="stat-card"/g,
    excludeFile: /components-stats-card\.css$/,
        fix: '使用 <stat-cards :cards="statsCards" :active-key.sync="activeFilter" @card-click="onCardClick" />；自定义色类用 card.colorClass（如 pending/cooking/alarm），卡片追加内容用 #card-append 插槽'
  },
  W002: {
    level: 'WARNING',
    desc: '手写 filter-bar/cs-filter 应改用 table-bar 组件',
    pattern: /\bclass="[^"]*(filter-bar|cs-filter|search-bar)[^"]*"/g,
    excludeFile: /components\.css$/,
    fix: '使用 <table-bar :search-items="searchConfig" :actions="actionConfig" @search="onSearch" />'
  },
  W003: {
    level: 'WARNING',
    desc: 'label-width 非 100px（表单标签宽度不统一）',
    pattern: /label-width="(\d+)px"/g,
    check: (match, fullContent) => {
      const width = match[1];
      return width !== '100';
    },
    fix: '统一使用 label-width="100px"，长标签场景可局部调至 120px 但需同模块内保持一致'
  },
  W004: {
    level: 'WARNING',
    desc: '硬编码十六进制色值（应使用 CSS 变量/设计令牌）',
    // 匹配 style 属性或 <style> 块中的 hex 色值，排除 CSS 变量定义本身
    pattern: /(?:color|background|border|fill|stroke|outline)\s*:\s*[^;]*#([0-9a-fA-F]{3,8})\b/g,
    check: (match, fullContent) => {
      // 排除 CSS 变量定义行（如 --color-brand-500: #ffc200）
      const line = fullContent.substring(Math.max(0, fullContent.lastIndexOf('\n', match.index)), match.index + 50);
      return !line.includes('--');
    },
    fix: '使用 var(--color-xxx) 或 var(--el-color-xxx) 设计令牌替代硬编码色值'
  },
  W005: {
    level: 'WARNING',
    desc: 'class="container" 缺少 ds-page（页面内边距不统一）',
    pattern: /class="container"\s/g,
    fix: '改用 class="container ds-page"，确保全站页面内边距一致'
  },
  W006: {
    level: 'WARNING',
    desc: 'aria-label="页面主要内容" 应改用 aria-labelledby="page-title"',
    pattern: /aria-label="页面主要内容"/g,
    fix: '改用 aria-labelledby="page-title" 并添加 <h1 class="sr-only" id="page-title">页面标题</h1>'
  }
};

// ── 扫描引擎 ──────────────────────────────────────────
let totalErrors = 0;
let totalWarnings = 0;

function scanFile(filePath, relPath) {
  const content = fs.readFileSync(filePath, 'utf-8');
  const results = [];

  for (const [code, rule] of Object.entries(rules)) {
    if (quiet && rule.level === 'WARNING') continue;

    // 文件级排除
    if (rule.excludeFile && rule.excludeFile.test(relPath)) continue;

    // 重置正则 lastIndex
    rule.pattern.lastIndex = 0;
    let match;

    while ((match = rule.pattern.exec(content)) !== null) {
      // 行级排除
      if (rule.exclude) {
        const lineStart = content.lastIndexOf('\n', match.index) + 1;
        const lineEnd = content.indexOf('\n', match.index);
        const line = content.substring(lineStart, lineEnd === -1 ? content.length : lineEnd);
        if (rule.exclude.test(line)) continue;
      }

      // 自定义检查函数
      if (rule.check && !rule.check(match, content)) continue;

      // 计算行号
      const lineNum = content.substring(0, match.index).split('\n').length;

      results.push({
        code,
        level: rule.level,
        line: lineNum,
        desc: rule.desc,
        match: match[0].substring(0, 80),
        fix: showFix ? rule.fix : null
      });
    }
  }

  return results;
}

function formatResult(relPath, result) {
  const prefix = result.level === 'ERROR' ? '✗' : '⚠';
  let line = `  ${prefix} ${result.code}  L${result.line}  ${result.desc}`;
  if (result.match) line += `\n      匹配: ${result.match}`;
  if (result.fix) line += `\n      修复: ${result.fix}`;
  return line;
}

// ── 主流程 ────────────────────────────────────────────
function main() {
  console.log('\n╔══════════════════════════════════════════╗');
  console.log('║   UI 规范检测 (lint-ui-conventions)      ║');
  console.log('╚══════════════════════════════════════════╝\n');

  const htmlFiles = [];
  const cssFiles = [];

  // 递归收集 HTML 和 CSS 文件
  function walkDir(dir) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        // 跳过 _templates 目录
        if (entry.name === '_templates') continue;
        walkDir(fullPath);
      } else if (entry.name.endsWith('.html')) {
        htmlFiles.push(fullPath);
      } else if (entry.name.endsWith('.css')) {
        cssFiles.push(fullPath);
      }
    }
  }

  walkDir(PAGE_DIR);

  // 也扫描 styles 目录
  if (fs.existsSync(STYLE_DIR)) {
    const styleEntries = fs.readdirSync(STYLE_DIR, { withFileTypes: true });
    for (const entry of styleEntries) {
      if (entry.isFile() && entry.name.endsWith('.css')) {
        cssFiles.push(path.join(STYLE_DIR, entry.name));
      }
    }
  }

  console.log(`扫描范围: ${htmlFiles.length} 个 HTML 文件, ${cssFiles.length} 个 CSS 文件\n`);

  let hasAnyIssue = false;

  // 扫描 HTML 文件
  for (const filePath of htmlFiles) {
    const relPath = path.relative(PAGE_DIR, filePath);
    const results = scanFile(filePath, relPath);

    if (results.length > 0) {
      hasAnyIssue = true;
      const errors = results.filter(r => r.level === 'ERROR');
      const warnings = results.filter(r => r.level === 'WARNING');
      totalErrors += errors.length;
      totalWarnings += warnings.length;

      console.log(`📄 ${relPath}`);
      for (const r of results) {
        console.log(formatResult(relPath, r));
      }
      console.log('');
    }
  }

  // 扫描 CSS 文件（仅 W004 硬编码色值规则）
  for (const filePath of cssFiles) {
    const relPath = path.relative(path.resolve(__dirname, '..'), filePath);
    const content = fs.readFileSync(filePath, 'utf-8');

    if (quiet) continue; // W004 是 WARNING 级别

    const pattern = rules.W004.pattern;
    pattern.lastIndex = 0;
    let match;
    const cssResults = [];

    while ((match = pattern.exec(content)) !== null) {
      if (rules.W004.check && !rules.W004.check(match, content)) continue;
      const lineNum = content.substring(0, match.index).split('\n').length;
      cssResults.push({
        code: 'W004',
        level: 'WARNING',
        line: lineNum,
        desc: rules.W004.desc,
        match: match[0].substring(0, 80),
        fix: showFix ? rules.W004.fix : null
      });
    }

    if (cssResults.length > 0) {
      hasAnyIssue = true;
      totalWarnings += cssResults.length;
      console.log(`🎨 ${relPath}`);
      for (const r of cssResults) {
        console.log(formatResult(relPath, r));
      }
      console.log('');
    }
  }

  // ── 汇总 ──
  console.log('──────────────────────────────────────────');
  console.log(`  合计: ${totalErrors} 个 ERROR, ${totalWarnings} 个 WARNING`);
  console.log('──────────────────────────────────────────\n');

  if (totalErrors > 0) {
    console.log('❌ 检测未通过：存在 ERROR 级别违规，请修复后再提交。\n');
    process.exit(1);
  } else if (totalWarnings > 0) {
    console.log('⚠️  检测通过（有 WARNING）：建议修复 WARNING 级别问题以保持设计一致性。\n');
    process.exit(0);
  } else {
    console.log('✅ 检测通过：所有页面符合 UI 规范。\n');
    process.exit(0);
  }
}

main();