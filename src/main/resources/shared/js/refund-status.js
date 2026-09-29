/**
 * 退款 / 售后状态单一真源（与后端 com.reggie.enums.RefundStatus 的 code 对齐）。
 *
 * 背景（P0-5 三端语义落地）：后端枚举 code 大小写并不统一——
 * pending / processing / fail / rejected 为小写，而 SUCCESS 为大写。
 * 历史上各端各自内联一份映射，一旦漏配某个大小写就会把原始码（如 "PENDING"）
 * 直接暴露给顾客，形成"无回执"体验。
 *
 * 因此本字典按「大写归一化」查表，对大小写不敏感，三端（顾客端 / 后台）共用，
 * 杜绝状态文案各自硬编码导致的不一致。仅做文案 / 样式映射，不含业务逻辑。
 *
 * 修改点(P0-5)：新增本文件作为三端共用退款状态字典。
 */
(function (win) {
  'use strict';

  var REFUND = {
    PENDING:   { code: 'pending',    text: '退款申请中', cls: 'st-pending' },
    PROCESSING:{ code: 'processing', text: '退款处理中', cls: 'st-processing' },
    SUCCESS:   { code: 'SUCCESS',    text: '退款成功',   cls: 'st-success' },
    FAIL:      { code: 'fail',       text: '退款失败',   cls: 'st-fail' },
    REJECTED:  { code: 'rejected',   text: '审核拒绝',   cls: 'st-rejected' }
  };

  /** 归一化：大小写与上空格不敏感（兼容后端 SUCCESS 大写、其余小写） */
  function normalize(status) {
    return status == null ? '' : String(status).trim().toUpperCase();
  }

  function refundText(status) {
    var m = REFUND[normalize(status)];
    return m ? m.text : (status ? String(status) : '未知');
  }

  function refundClass(status) {
    var m = REFUND[normalize(status)];
    return m ? m.cls : '';
  }

  /** 是否为终态（成功 / 失败 / 拒绝），供前端决定是否展示"再次申请"入口 */
  function isFinal(status) {
    var k = normalize(status);
    return k === 'SUCCESS' || k === 'FAIL' || k === 'REJECTED';
  }

  win.RefundStatusDict = {
    REFUND: REFUND,
    refundText: refundText,
    refundClass: refundClass,
    isFinal: isFinal
  };
})(window);
