// 满减营销 API 模块（C端 /api/marketing/full-reduction 系列）
// 试算结果与下单计费同源，进度条/凑单提示一律以此为准，不在前端本地另算优惠

// 查询当前生效满减档位
function getFullReductionTiers() {
  return $axios({
    url: '/api/marketing/full-reduction/tiers',
    method: 'get'
  })
}

// 满减试算（进度条/凑单；amount=商品金额，不含配送费）
function evaluateFullReduction(amount) {
  return $axios({
    url: '/api/marketing/full-reduction/evaluate',
    method: 'get',
    params: { amount: amount }
  })
}
