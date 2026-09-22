package com.reggie.module.address.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.LogMaskUtils;
import com.reggie.common.R;
import com.reggie.module.address.model.AddressBook;
import com.reggie.module.address.service.AddressBookService;
import com.reggie.utils.GeoUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 地址簿管理
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/address-book")
@Tag(name = "地址簿管理", description = "用户地址簿CRUD接口")
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private GeoUtils geoUtils;

    // 仅"纯数字（含全角）"或"数字+单个字母"才自动补后缀；文本类原值返回
    private static final Pattern AUTO_SUFFIX_PATTERN =
            Pattern.compile("[0-9０-９]+[A-Za-zＡ-Ｚａ-ｚ]?");

    // detail 列长度，须与迁移脚本及测试 schema 保持一致
    private static final int DETAIL_MAX_LENGTH = 255;

    /**
     * 拼接完整地址用于地理编码（省+市+区+详细）
     */
    private String buildFullAddress(AddressBook addressBook) {
        StringBuilder sb = new StringBuilder();
        if (addressBook.getProvinceName() != null) sb.append(addressBook.getProvinceName());
        if (addressBook.getCityName() != null) sb.append(addressBook.getCityName());
        if (addressBook.getDistrictName() != null) sb.append(addressBook.getDistrictName());
        if (addressBook.getDetail() != null) sb.append(addressBook.getDetail());
        return sb.toString();
    }

    /**
     * 按结构化字段规范化拼接完整地址，覆盖前端传入的 detail，保证全链路（订单快照/配送/打印）口径统一。
     * 参考美团/淘宝：街道、小区直接相连（中文可读），楼栋+单元+楼层+门牌紧凑拼接；
     * 用户只填数字时自动补标准后缀，已含后缀（如"5号楼/A座/1503室"）则不重复补。
     */
    private String buildStructuredDetail(AddressBook ab) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, ab.getStreetName());
        appendPart(sb, ab.getCommunity());
        appendPart(sb, ensureSuffix(ab.getBuilding(), "栋"));
        appendPart(sb, ensureSuffix(ab.getUnit(), "单元"));
        appendPart(sb, ensureSuffix(ab.getFloor(), "层"));
        appendPart(sb, ensureSuffix(ab.getRoomNo(), "室"));
        String result = sb.toString();
        // 兜底：极端超长时截断，防止写入超过 detail 列长导致 500
        if (result.length() > DETAIL_MAX_LENGTH) {
            log.warn("结构化地址超过{}字符，已截断: {}", DETAIL_MAX_LENGTH, result);
            result = result.substring(0, DETAIL_MAX_LENGTH);
        }
        return result;
    }

    private void appendPart(StringBuilder sb, String part) {
        if (part != null) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sb.append(trimmed);
            }
        }
    }

    /**
     * 仅当值为"纯数字（含全角）"或"数字+单个字母"时补标准后缀（3→3栋、1503→1503室）；
     * 文本类（如"东门/A区/裙房"）及已自带后缀的值（"5号楼/1503室"）原样返回，
     * 避免"东门栋""前台室"这类错误后缀。
     */
    private String ensureSuffix(String value, String defaultSuffix) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (AUTO_SUFFIX_PATTERN.matcher(trimmed).matches()) {
            return trimmed + defaultSuffix;
        }
        return trimmed;
    }

    private boolean isFilled(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 是否填了任一结构化字段
     */
    private boolean hasAnyStructured(AddressBook ab) {
        return isFilled(ab.getStreetName()) || isFilled(ab.getCommunity())
                || isFilled(ab.getBuilding()) || isFilled(ab.getUnit())
                || isFilled(ab.getFloor()) || isFilled(ab.getRoomNo());
    }

    /**
     * 结构化地址是否达到"可定位"的最小要求：有小区/大厦，或 楼栋+门牌号 同时填写。
     * 仅填孤立门牌（如"1503室"）不足以定位。
     */
    private boolean hasEnoughStructured(AddressBook ab) {
        return isFilled(ab.getCommunity())
                || (isFilled(ab.getBuilding()) && isFilled(ab.getRoomNo()));
    }

    /**
     * 是否为旧格式地址：六个结构化列全空、仅有 detail 文本
     */
    private boolean isLegacyAddress(AddressBook existing) {
        return !hasAnyStructured(existing) && isFilled(existing.getDetail());
    }

    /**
     * 合并字段：入参非 null（含空串=清空）以入参为准，入参 null（部分更新未传）保持原值。
     * 前端编辑保存时为全量提交，故正常路径下取入参值。
     */
    private String mergeValue(String input, String existing) {
        if (input == null) {
            return existing;
        }
        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 新增地址
     *
     * @param addressBook 地址信息
     * @return 新增地址信息
     */
    @PostMapping
    @Operation(summary = "新增地址", description = "添加新的收货地址，自动关联当前用户")
    @Parameter(name = "addressBook", description = "地址信息（收货人、手机号、详细地址等）", required = true)
    public R<AddressBook> save(@Valid @RequestBody AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setTenantId(BaseContext.getCurrentTenantId());
        // 按结构化字段（街道/小区/栋/单元/层/门牌）规范化生成 detail；全链路统一读 detail
        String structuredDetail = buildStructuredDetail(addressBook);
        if (structuredDetail.isEmpty() || !hasEnoughStructured(addressBook)) {
            return R.error("请填写小区/大厦，或同时填写楼栋和门牌号");
        }
        addressBook.setDetail(structuredDetail);
        log.info("新增地址，手机号：{}，地址：{}",
            LogMaskUtils.maskPhone(addressBook.getPhone()),
            LogMaskUtils.maskAddress(addressBook.getDetail()));
        // 自动地理编码回填经纬度（Key 未配置时降级为空，不阻断保存）
        BigDecimal[] lngLat = geoUtils.geocode(buildFullAddress(addressBook));
        if (lngLat != null) {
            addressBook.setLongitude(lngLat[0]);
            addressBook.setLatitude(lngLat[1]);
        }
        // 修改点：首个地址自动设为默认——新用户首次添加地址时 isDefault 未设置（前端表单无此字段），
        // 导致 getDefault() 永远返回空，结算页反复跳转地址编辑页无法完成下单。
        Long userId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        boolean hasExisting = addressBookService.lambdaQuery()
                .eq(AddressBook::getUserId, userId)
                .eq(tenantId != null, AddressBook::getTenantId, tenantId)
                .exists();
        if (!hasExisting) {
            addressBook.setIsDefault(1);
        }
        addressBookService.save(addressBook);
        return R.success(addressBook);
    }

    /**
     * 修改地址信息
     *
     * @param addressBook 地址信息
     * @return 修改结果
     */
    @PutMapping
    @Operation(summary = "修改地址", description = "更新地址信息，自动校验租户权限")
    public R<AddressBook> update(@Parameter(description = "地址信息（含ID）", required =
            true) @Valid @RequestBody AddressBook addressBook) {
        // 属主 + 租户校验：地址真正按 user_id 隔离（同租户顾客共享 tenantId），二者缺一即拒绝
        AddressBook existing = addressBookService.getById(addressBook.getId());
        Long currentUserId = BaseContext.getCurrentId();
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (existing == null || currentUserId == null || !currentUserId.equals(existing.getUserId())
                || (currentTenantId != null && !currentTenantId.equals(existing.getTenantId()))) {
            return R.error("没有查询到对应地址信息");
        }
        // 合并结构化字段：前端全量提交；部分更新缺字段(null)时保持原值，空串=清空
        String streetName = mergeValue(addressBook.getStreetName(), existing.getStreetName());
        String community = mergeValue(addressBook.getCommunity(), existing.getCommunity());
        String building = mergeValue(addressBook.getBuilding(), existing.getBuilding());
        String unit = mergeValue(addressBook.getUnit(), existing.getUnit());
        String floor = mergeValue(addressBook.getFloor(), existing.getFloor());
        String roomNo = mergeValue(addressBook.getRoomNo(), existing.getRoomNo());
        AddressBook structured = new AddressBook();
        structured.setStreetName(streetName);
        structured.setCommunity(community);
        structured.setBuilding(building);
        structured.setUnit(unit);
        structured.setFloor(floor);
        structured.setRoomNo(roomNo);
        String structuredDetail;
        if (!hasAnyStructured(structured)) {
            // 本次未填任何结构化信息
            if (isLegacyAddress(existing)) {
                // 旧地址仅改联系人/电话：保留原 detail，六列维持 null（与现状一致）
                structuredDetail = existing.getDetail().trim();
            } else {
                // 新格式地址被清空全部定位信息：阻断，避免产生"六列空+detail残留"的不一致
                return R.error("详细地址不能为空");
            }
        } else {
            // 填了结构化信息：必须达到可定位要求，防止老地址只补零碎门牌导致地址退化
            if (!hasEnoughStructured(structured)) {
                return R.error("请补全小区/大厦，或同时填写楼栋和门牌号");
            }
            structuredDetail = buildStructuredDetail(structured);
        }
        // 白名单更新：省/市/区 code+name + 结构化6字段 + detail，租户条件防跨租户越权
        LambdaUpdateWrapper<AddressBook> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AddressBook::getId, addressBook.getId())
                .eq(AddressBook::getUserId, currentUserId)
                .eq(AddressBook::getTenantId, currentTenantId);
        if (addressBook.getConsignee() != null) wrapper.set(AddressBook::getConsignee, addressBook.getConsignee());
        if (addressBook.getPhone() != null) wrapper.set(AddressBook::getPhone, addressBook.getPhone());
        if (addressBook.getProvinceCode() != null) wrapper.set(AddressBook::getProvinceCode, addressBook.getProvinceCode());
        if (addressBook.getProvinceName() != null) wrapper.set(AddressBook::getProvinceName, addressBook.getProvinceName());
        if (addressBook.getCityCode() != null) wrapper.set(AddressBook::getCityCode, addressBook.getCityCode());
        if (addressBook.getCityName() != null) wrapper.set(AddressBook::getCityName, addressBook.getCityName());
        if (addressBook.getDistrictCode() != null) wrapper.set(AddressBook::getDistrictCode, addressBook.getDistrictCode());
        if (addressBook.getDistrictName() != null) wrapper.set(AddressBook::getDistrictName, addressBook.getDistrictName());
        // 结构化6字段与 detail 无条件 set（值为 null 即清空），保证编辑结果与落库一致
        wrapper.set(AddressBook::getStreetName, streetName)
                .set(AddressBook::getCommunity, community)
                .set(AddressBook::getBuilding, building)
                .set(AddressBook::getUnit, unit)
                .set(AddressBook::getFloor, floor)
                .set(AddressBook::getRoomNo, roomNo)
                .set(AddressBook::getDetail, structuredDetail);
        if (addressBook.getLabel() != null) wrapper.set(AddressBook::getLabel, addressBook.getLabel());
        if (addressBook.getIsDefault() != null) wrapper.set(AddressBook::getIsDefault, addressBook.getIsDefault());
        // 地址内容变化时重新地理编码：用合并后的完整地址（省市区 + 规范化 detail）
        boolean addressChanged = addressBook.getProvinceName() != null || addressBook.getCityName() != null
                || addressBook.getDistrictName() != null || addressBook.getStreetName() != null
                || addressBook.getCommunity() != null || addressBook.getBuilding() != null
                || addressBook.getUnit() != null || addressBook.getFloor() != null
                || addressBook.getRoomNo() != null;
        if (addressChanged) {
            AddressBook forGeo = new AddressBook();
            forGeo.setProvinceName(mergeValue(addressBook.getProvinceName(), existing.getProvinceName()));
            forGeo.setCityName(mergeValue(addressBook.getCityName(), existing.getCityName()));
            forGeo.setDistrictName(mergeValue(addressBook.getDistrictName(), existing.getDistrictName()));
            forGeo.setDetail(structuredDetail);
            BigDecimal[] lngLat = geoUtils.geocode(buildFullAddress(forGeo));
            if (lngLat != null) {
                wrapper.set(AddressBook::getLongitude, lngLat[0]).set(AddressBook::getLatitude, lngLat[1]);
            }
        }
        addressBookService.update(wrapper);
        // 同步响应对象的 detail，保证回显与落库一致
        addressBook.setDetail(structuredDetail);
        return R.success(addressBook);
    }

    /**
     * 批量删除地址
     *
     * @param ids 地址ID列表
     * @return 删除结果
     */
    @DeleteMapping
    @Operation(summary = "删除地址", description = "批量删除地址，自动校验租户权限")
    @Parameter(name = "ids", description = "地址ID列表", required = true)
    public R<String> delete(@RequestParam List<Long> ids) {
        // 属主 + 租户校验，防止同租户跨用户删除他人地址
        Long currentUserId = BaseContext.getCurrentId();
        Long currentTenantId = BaseContext.getCurrentTenantId();
        for (Long id : ids) {
            AddressBook addressBook = addressBookService.getById(id);
            if (addressBook == null || currentUserId == null
                    || !currentUserId.equals(addressBook.getUserId())
                    || (currentTenantId != null && !currentTenantId.equals(addressBook.getTenantId()))) {
                return R.error("地址ID " + id + " 不属于当前用户");
            }
        }
        addressBookService.removeByIds(ids);
        return R.success("删除成功");
    }

    /**
     * 查询最后更新的地址
     *
     * @return 最后更新的地址
     */
    @GetMapping("/lastUpdate")
    @Operation(summary = "查询最后更新的地址", description = "查询用户最近更新的地址")
    public R<AddressBook> lastUpdate() {
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        Long currentTenantId = BaseContext.getCurrentTenantId();
        queryWrapper.eq(currentTenantId != null, AddressBook::getTenantId, currentTenantId);
        queryWrapper.orderByDesc(AddressBook::getUpdateTime);
        queryWrapper.last("LIMIT 1");
        AddressBook addressBook = addressBookService.getOne(queryWrapper);
        if (addressBook != null) {
            return R.success(addressBook);
        }
        return R.error("没有找到该对象");
    }

    /**
     * 设置默认地址
     *
     * @param addressBook 地址信息
     * @return 更新后的地址信息
     */
    @PutMapping("default")
    @Operation(summary = "设置默认地址", description = "将指定地址设为默认收货地址")
    @Parameter(name = "addressBook", description = "地址信息", required = true)
    public R<AddressBook> setDefault(@RequestBody AddressBook addressBook) {
        log.info("设置默认地址，手机号：{}，地址：{}",
            LogMaskUtils.maskPhone(addressBook.getPhone()),
            LogMaskUtils.maskAddress(addressBook.getDetail()));
        // 属主 + 租户校验
        AddressBook existing = addressBookService.getById(addressBook.getId());
        Long currentUserId = BaseContext.getCurrentId();
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (existing == null || currentUserId == null || !currentUserId.equals(existing.getUserId())
                || (currentTenantId != null && !currentTenantId.equals(existing.getTenantId()))) {
            return R.error("没有查询到对应地址信息");
        }
        LambdaUpdateWrapper<AddressBook> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AddressBook::getUserId, currentUserId);
        wrapper.eq(currentTenantId != null, AddressBook::getTenantId, currentTenantId);
        wrapper.set(AddressBook::getIsDefault, AddressBook.NOT_DEFAULT);
        //SQL:update address_book set is_default = 0 where user_id = ? and tenant_id = ?
        addressBookService.update(wrapper);

        // 使用白名单字段更新，防止 updateById 全字段覆盖
        LambdaUpdateWrapper<AddressBook> wrapper2 = new LambdaUpdateWrapper<>();
        wrapper2.eq(AddressBook::getId, addressBook.getId())
                .eq(AddressBook::getUserId, currentUserId)
                .eq(currentTenantId != null, AddressBook::getTenantId, currentTenantId)
                .set(AddressBook::getIsDefault, AddressBook.IS_DEFAULT);
        addressBookService.update(wrapper2);
        addressBook.setIsDefault(AddressBook.IS_DEFAULT);
        return R.success(addressBook);
    }

    /**
     * 根据ID查询地址
     *
     * @param id 地址ID
     * @return 地址信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询地址详情", description = "根据ID查询地址信息")
    @Parameter(name = "id", description = "地址ID", required = true)
    public R<AddressBook> get(@PathVariable Long id) {
        AddressBook addressBook = addressBookService.getById(id);
        // 属主 + 租户校验：防止同租户跨用户读取他人地址（收货人/手机号/地址等 PII）
        Long currentUserId = BaseContext.getCurrentId();
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (addressBook != null && (currentUserId == null
                || !currentUserId.equals(addressBook.getUserId())
                || currentTenantId == null
                || !currentTenantId.equals(addressBook.getTenantId()))) {
            return R.error("没有查询到对应地址信息");
        }
        if (addressBook != null) {
            return R.success(addressBook);
        } else {
            return R.error("没有找到该对象");
        }
    }

    /**
     * 查询默认地址
     *
     * @return 默认地址信息
     */
    @GetMapping("default")
    @Operation(summary = "查询默认地址", description = "查询用户的默认收货地址")
    public R<AddressBook> getDefault() {
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        queryWrapper.eq(AddressBook::getIsDefault, 1);
        Long currentTenantId = BaseContext.getCurrentTenantId();
        queryWrapper.eq(currentTenantId != null, AddressBook::getTenantId, currentTenantId);

        //SQL:select * from address_book where user_id = ? and is_default = 1 and tenant_id = ?
        AddressBook addressBook = addressBookService.getOne(queryWrapper);

        if (addressBook == null) {
            return R.error("没有找到该对象");
        } else {
            return R.success(addressBook);
        }
    }

    /**
     * 查询指定用户的全部地址
     *
     * @param addressBook 地址查询条件
     * @return 地址列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询地址列表", description = "查询用户的所有地址")
    @Parameter(name = "addressBook", description = "地址查询条件")
    public R<List<AddressBook>> list(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        log.info("查询地址列表，手机号：{}，地址：{}",
            LogMaskUtils.maskPhone(addressBook.getPhone()),
            LogMaskUtils.maskAddress(addressBook.getDetail()));

        //条件构造器
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(null != addressBook.getUserId(), AddressBook::getUserId, addressBook.getUserId());
        Long currentTenantId = BaseContext.getCurrentTenantId();
        queryWrapper.eq(currentTenantId != null, AddressBook::getTenantId, currentTenantId);
        queryWrapper.orderByDesc(AddressBook::getUpdateTime);

        //SQL:select * from address_book where user_id = ? and tenant_id = ? order by update_time desc
        return R.success(addressBookService.list(queryWrapper));
    }
}


