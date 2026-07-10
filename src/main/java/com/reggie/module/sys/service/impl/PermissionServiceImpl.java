package com.reggie.module.sys.service.impl;

import com.reggie.module.sys.entity.Permission;
import com.reggie.module.sys.mapper.PermissionMapper;
import com.reggie.module.sys.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 */
@Slf4j
@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PERMISSION_CACHE_KEY = "sys:permissions:";

    @Override
    public List<Permission> getAllPermissions() {
        return permissionMapper.listAllEnabled();
    }

    @Override
    public List<Permission> getPermissionsByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        String ids = roleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String inSql = "SELECT permission_id FROM role_permission WHERE role_id IN (" + ids + ")";

        List<Permission> result = permissionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Permission>()
                .inSql(Permission::getId, inSql)
                .eq(Permission::getStatus, 1)
                .orderByAsc(Permission::getSort)
        );
        return result != null ? result : Collections.emptyList();
    }

    @Override
    public List<String> getPermissionKeys(Long roleId) {
        String cacheKey = PERMISSION_CACHE_KEY + roleId;
        List<String> cached = (List<String>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // 查询该角色的所有权限
        List<Permission> perms = getPermissionsByRoleIds(Collections.singletonList(roleId));
        List<String> keys = perms.stream()
                .map(Permission::getPermissionKey)
                .collect(Collectors.toList());

        if (!keys.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, keys, 1, TimeUnit.HOURS);
        }
        return keys;
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
