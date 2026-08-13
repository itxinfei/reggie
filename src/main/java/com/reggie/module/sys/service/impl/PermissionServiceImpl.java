package com.reggie.module.sys.service.impl;

import com.reggie.module.sys.entity.Permission;
import com.reggie.module.sys.mapper.PermissionMapper;
import com.reggie.module.sys.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 权限服务实现
 * 支持Redis缓存，权限变更时自动清除缓存
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /** 权限缓存Key前缀 */
    private static final String PERMISSION_CACHE_KEY = "sys:permissions:";

    /** 缓存过期时间（小时） */
    private static final long CACHE_TTL_HOURS = 1;

    @Override
    public List<Permission> getAllPermissions() {
        return permissionMapper.listAllEnabled();
    }

    @Override
    public List<Permission> getPermissionsByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 修改点：改为调用 PermissionMapper.listByRoleIds 关联查询。
        // 原 inSql("... WHERE role_id IN (?)") 中的 ? 为字面量、从未被绑定，会导致
        // PreparedStatement 参数缺失异常；且 permission/role_permission 无 tenant_id 列，
        // 使用 MP selectList 会被 TenantLineInnerInterceptor 追加 tenant_id 过滤而报错。
        List<Permission> result = permissionMapper.listByRoleIds(roleIds);
        return result != null ? result : Collections.emptyList();
    }

    @Override
    public List<String> getPermissionKeys(Long roleId) {
        // Redis不可用时直接查数据库
        if (redisTemplate == null) {
            return getPermissionKeysFromDb(roleId);
        }

        String cacheKey = PERMISSION_CACHE_KEY + roleId;
        try {
            List<String> cached = (List<String>) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("[权限缓存] 读取缓存失败，降级查数据库：{}", e.getMessage());
        }

        // 查询该角色的所有权限
        List<String> keys = getPermissionKeysFromDb(roleId);

        // 缓存结果（包括空列表，防止缓存穿透）
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, keys, CACHE_TTL_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("[权限缓存] 写入缓存失败：{}", e.getMessage());
            }
        }
        return keys;
    }

    /**
     * 从数据库查询权限Key
     */
    private List<String> getPermissionKeysFromDb(Long roleId) {
        List<Permission> perms = getPermissionsByRoleIds(Collections.singletonList(roleId));
        return perms.stream()
                .map(Permission::getPermissionKey)
                .collect(Collectors.toList());
    }

    /**
     * 清除指定角色的权限缓存
     * 权限变更时调用，确保缓存与数据库一致
     *
     * @param roleId 角色ID
     */
    public void clearPermissionCache(Long roleId) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String cacheKey = PERMISSION_CACHE_KEY + roleId;
            redisTemplate.delete(cacheKey);
            log.info("[权限缓存] 已清除角色权限缓存：roleId={}", roleId);
        } catch (Exception e) {
            log.warn("[权限缓存] 清除缓存失败：roleId={}, error={}", roleId, e.getMessage());
        }
    }

    /**
     * 清除所有权限缓存
     * 批量权限变更时调用
     * 修改点：使用 SCAN 替代 KEYS，通过 RedisCallback 执行，避免阻塞 Redis
     */
    public void clearAllPermissionCache() {
        if (redisTemplate == null) {
            return;
        }
        try {
            // 修改点：使用 SCAN 命令替代 KEYS 命令
            String pattern = PERMISSION_CACHE_KEY + "*";
            Set<String> keys = redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<Set<String>>) connection -> {
                    Set<String> result = new HashSet<>();
                    ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
                    Cursor<byte[]> cursor = connection.scan(options);
                    try {
                        while (cursor.hasNext()) {
                            result.add(new String(cursor.next()));
                        }
                    } finally {
                        try {
                            cursor.close();
                        } catch (Exception ignored) {
                            // cursor close silently
                        }
                    }
                    return result;
                }
            );
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("[权限缓存] 已清除所有权限缓存，共{}条", keys.size());
            }
        } catch (Exception e) {
            log.warn("[权限缓存] 清除所有缓存失败：{}", e.getMessage());
        }
    }

    @Override
    public List<String> getPermissionKeysByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Permission> perms = getPermissionsByRoleIds(roleIds);
        return perms.stream()
                .map(Permission::getPermissionKey)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getMenuTree(Long roleId) {
        List<Permission> allPerms = getAllPermissions();
        // 获取该角色的权限
        List<Long> roleIds = Collections.singletonList(roleId);
        List<Permission> rolePerms = getPermissionsByRoleIds(roleIds);
        Set<Long> rolePermIds = rolePerms.stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());

        // 只保留菜单类型(parent_id=0的顶级+该角色有权限的子项)
        List<Permission> menus = allPerms.stream()
                .filter(p -> p.getPermissionType() == Permission.TYPE_MENU)
                .filter(p -> p.getParentId() == 0 || rolePermIds.contains(p.getParentId()))
                .sorted(Comparator.comparing(Permission::getSort))
                .collect(Collectors.toList());

        // 构建树
        List<Map<String, Object>> tree = new ArrayList<>();
        for (Permission menu : menus) {
            if (menu.getParentId() == 0) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", menu.getId());
                node.put("name", menu.getPermissionName());
                node.put("icon", menu.getIcon());
                node.put("routePath", menu.getRoutePath());

                // 子菜单
                List<Permission> children = allPerms.stream()
                        .filter(p -> p.getParentId().equals(menu.getId()))
                        .filter(p -> rolePermIds.contains(p.getId()))
                        .sorted(Comparator.comparing(Permission::getSort))
                        .collect(Collectors.toList());

                List<Map<String, Object>> childList = children.stream().map(child -> {
                    Map<String, Object> childNode = new LinkedHashMap<>();
                    childNode.put("id", child.getId());
                    childNode.put("name", child.getPermissionName());
                    childNode.put("icon", child.getIcon());
                    childNode.put("routePath", child.getRoutePath());
                    return childNode;
                }).collect(Collectors.toList());

                node.put("children", childList);
                tree.add(node);
            }
        }
        return tree;
    }
}






