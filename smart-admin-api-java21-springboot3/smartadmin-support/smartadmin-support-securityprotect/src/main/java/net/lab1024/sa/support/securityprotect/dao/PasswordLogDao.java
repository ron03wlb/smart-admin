package net.lab1024.sa.support.securityprotect.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import net.lab1024.sa.support.securityprotect.domain.entity.PasswordLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 密码修改日志
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2024/7/15 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface PasswordLogDao extends BaseMapper<PasswordLogEntity> {

  /**
   * 查询最后一次修改密码记录
   *
   * @param userType 用户类型
   * @param userId 用户ID
   * @return 密码修改记录
   */
  PasswordLogEntity selectLastByUserTypeAndUserId(
      @Param("userType") Integer userType, @Param("userId") Long userId);

  /**
   * 查询最近几次修改后的密码
   *
   * @param userType 用户类型
   * @param userId 用户ID
   * @param limit 限制数量
   * @return 历史密码列表
   */
  List<String> selectOldPassword(
      @Param("userType") Integer userType, @Param("userId") Long userId, @Param("limit") int limit);
}
