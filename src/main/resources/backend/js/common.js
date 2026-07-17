var web_prefix = '/backend'

/*
 * ReggieUI 已统一迁移至 js/request.js（全局请求入口，所有业务页均引入）。
 * 业务页交互反馈请统一调用 window.ReggieUI（success/error/warning/info/
 * message/loading/confirm/notify），禁止混用 this.$message / ElMessage /
 * Notification 直写，确保提示样式与交互反馈一致、可统一管控。
 */