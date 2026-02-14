package net.lab1024.sa.system.role.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.system.role.domain.entity.RoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色 dao
 *
 * @author 1024创新实验室: 罗伊
 * @since 2022-02-26 21:34:01 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface RoleDao extends BaseMapper<RoleEntity> {

  /** 根据角色名称查询 */
  RoleEntity getByRoleName(@Param("roleName") String roleName);

  /** 根据角色编码 */
  RoleEntity getByRoleCode(@Param("roleCode") String roleCode);
}
