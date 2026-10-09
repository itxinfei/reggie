package com.reggie.module.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.sys.model.Department;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部门 Mapper
 * 简单 CRUD 走 BaseMapper，无自定义 SQL。
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}
