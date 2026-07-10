package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工Mapper接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee>{
}
