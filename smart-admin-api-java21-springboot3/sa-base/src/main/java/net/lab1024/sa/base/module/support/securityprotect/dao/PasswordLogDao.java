package net.lab1024.sa.base.module.support.securityprotect.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import net.lab1024.sa.base.module.support.securityprotect.domain.PasswordLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PasswordLogDao extends BaseMapper<PasswordLogEntity> {

  /**
   * 查询最后一次修改密码记录
   *
   * @param userType
   * @param userId
   * @return
   */
  PasswordLogEntity selectLastByUserTypeAndUserId(
      @Param("userType") Integer userType, @Param("userId") Long userId);

  /**
   * 查询最近几次修改后的密码
   *
   * @param userType
   * @param userId
   * @return
   */
  List<String> selectOldPassword(
      @Param("userType") Integer userType, @Param("userId") Long userId, @Param("limit") int limit);
}
