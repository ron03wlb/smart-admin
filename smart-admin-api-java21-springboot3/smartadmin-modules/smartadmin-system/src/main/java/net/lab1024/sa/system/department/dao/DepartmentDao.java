package net.lab1024.sa.system.department.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import net.lab1024.sa.system.department.domain.entity.DepartmentEntity;
import net.lab1024.sa.system.department.domain.vo.DepartmentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 部门
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-01-12 20:37:48 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface DepartmentDao extends BaseMapper<DepartmentEntity> {

  /** 根据部门id，查询此部门直接子部门的数量 */
  Integer countSubDepartment(@Param("departmentId") Long departmentId);

  /** 获取全部部门列表 */
  List<DepartmentVO> listAll();

  DepartmentVO selectDepartmentVO(@Param("departmentId") Long departmentId);
}
