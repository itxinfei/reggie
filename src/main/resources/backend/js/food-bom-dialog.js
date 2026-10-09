/**
 * 瑞吉外卖后台 — 菜品配方（BOM）编辑弹窗组件
 * 修改点(2026-09-24)：从 page/food/list.html（941 行）拆出独立组件文件，
 *   降低最高频菜品页的体积与回归风险；API 依赖 api/inventory.js 的
 *   materialList / dishMaterialListByDish / dishMaterialBatchSave（页面已引入）。
 *
 * 用法（food/list.html）：
 *   <food-bom-dialog ref="bomDialog" @saved="onBomSaved"></food-bom-dialog>
 *   打开：this.$refs.bomDialog.open(row)   // row: { id, name }
 */
(function (global) {
  'use strict';

  var FoodBomDialog = {
    name: 'FoodBomDialog',
    template:
      '<crud-dialog :show-submit="true" :show-cancel="true"' +
      '  size="lg"' +
      '  :title="\'编辑配方 - \' + (dishName || \'\')"' +
      '  :visible.sync="visible"' +
      '  :close-on-click-modal="false"' +
      '  :submit-loading="submitting"' +
      '  custom-class="bom-dialog"' +
      '  @submit="submit">' +
      '  <div class="bom-toolbar">' +
      '    <el-button type="primary" size="small" icon="ri-add-line" @click="addItem">添加食材</el-button>' +
      '    <el-button size="small" @click="loadItems">刷新</el-button>' +
      '  </div>' +
      '  <el-table :data="items" size="small" border style="width: 100%;">' +
      '    <el-table-column label="食材名称" width="180" align="center">' +
      '      <template slot-scope="scope">' +
      '        <el-select v-model="scope.row.materialId" placeholder="请选择食材" size="small" filterable' +
      '          @change="onMaterialChange(scope.row, $event)" style="width: 100%;">' +
      '          <el-option v-for="m in materialOptions" :key="m.id" :label="m.name" :value="m.id">' +
      '            <span>{{ m.name }}</span>' +
      '            <span class="ds-form-hint">{{ m.unit }}</span>' +
      '          </el-option>' +
      '        </el-select>' +
      '      </template>' +
      '    </el-table-column>' +
      '    <el-table-column label="用量" width="140" align="center">' +
      '      <template slot-scope="scope">' +
      '        <el-input-number v-model="scope.row.usageQty" :min="0.001" :precision="3"' +
      '          controls-position="right" size="small" style="width: 100%;" />' +
      '      </template>' +
      '    </el-table-column>' +
      '    <el-table-column label="单位" width="100" align="center" :header-align="\'center\'" :show-overflow-tooltip="true">' +
      '      <template slot-scope="scope">' +
      '        <span class="bom-unit">{{ scope.row.materialUnit || \'-\' }}</span>' +
      '      </template>' +
      '    </el-table-column>' +
      '    <el-table-column label="操作" align="center" width="120" :header-align="\'center\'">' +
      '      <template slot-scope="scope">' +
      '        <el-button type="text" size="small" class="btn-delete" @click="delItem(scope.$index)">删除</el-button>' +
      '      </template>' +
      '    </el-table-column>' +
      '  </el-table>' +
      '  <div class="ds-text-muted ds-text-xs bom-hint">' +
      '    提示：点击"保存"将提交全部食材配方（已保存的更新、新增的创建、删除的移除）。' +
      '  </div>' +
      '</crud-dialog>',
    data: function () {
      return {
        visible: false,
        dishId: null,
        dishName: '',
        items: [],
        materialOptions: [],
        submitting: false
      };
    },
    methods: {
      /** 对外入口：打开弹窗（row: { id, name }） */
      open: function (row) {
        row = row || {};
        this.dishId = row.id;
        this.dishName = row.name || '';
        this.visible = true;
        this.loadMaterialOptions();
        this.loadItems();
      },
      loadMaterialOptions: function () {
        var self = this;
        if (typeof materialList !== 'function') { return; }
        materialList().then(function (res) {
          if (res.code === 1) { self.materialOptions = res.data || []; }
        }).catch(function (err) { ReggieUI.error('加载食材列表失败：' + err); });
      },
      loadItems: function () {
        var self = this;
        var dishId = self.dishId;
        if (!dishId) { return; }
        if (typeof dishMaterialListByDish !== 'function') { return; }
        dishMaterialListByDish(dishId).then(function (res) {
          if (res.code === 1) {
            var data = res.data || [];
            self.items = data.map(function (item) {
              return {
                id: item.id || null,
                dishId: item.dishId || dishId,
                materialId: item.materialId || null,
                usageQty: item.usageQty != null ? parseFloat(item.usageQty) : 0.001,
                sort: item.sort || 0,
                materialUnit: item.materialUnit || ''
              };
            });
            if (self.items.length === 0) { self.addItem(); }
          } else { ReggieUI.error(res.msg || '加载配方失败'); }
        }).catch(function (err) { ReggieUI.error('请求失败：' + err); });
      },
      addItem: function () {
        this.items.push({
          id: null,
          dishId: this.dishId,
          materialId: null,
          usageQty: 0.001,
          sort: this.items.length,
          materialUnit: ''
        });
      },
      delItem: function (index) {
        if (this.items.length <= 1) { ReggieUI.warning('至少保留一条'); return; }
        this.items.splice(index, 1);
      },
      onMaterialChange: function (row, value) {
        for (var i = 0; i < this.materialOptions.length; i++) {
          if (this.materialOptions[i].id === value) {
            row.materialUnit = this.materialOptions[i].unit || '';
            break;
          }
        }
      },
      submit: function () {
        if (this.submitting) return;
        var self = this;
        // 校验：所有行必须选食材且用量 > 0
        for (var i = 0; i < self.items.length; i++) {
          var item = self.items[i];
          if (!item.materialId) { ReggieUI.warning('第 ' + (i + 1) + ' 行未选择食材'); return; }
          if (!item.usageQty || item.usageQty <= 0) { ReggieUI.warning('第 ' + (i + 1) + ' 行用量必须大于 0'); return; }
        }
        var items = self.items.map(function (it) {
          return {
            dishId: self.dishId,
            materialId: it.materialId,
            usageQty: it.usageQty,
            sort: it.sort
          };
        });
        var params = { dishId: self.dishId, items: items };
        self.submitting = true;
        if (typeof dishMaterialBatchSave !== 'function') { self.submitting = false; return; }
        dishMaterialBatchSave(params).then(function (res) {
          self.submitting = false;
          if (res.code === 1) {
            ReggieUI.success('配方保存成功！');
            self.visible = false;
            self.items = [];
            self.$emit('saved');
          } else { ReggieUI.error(res.msg || '保存失败'); }
        }).catch(function (err) {
          self.submitting = false;
          ReggieUI.error('请求失败：' + err);
        });
      }
    }
  };

  global.FoodBomDialog = FoodBomDialog;
})(window);
