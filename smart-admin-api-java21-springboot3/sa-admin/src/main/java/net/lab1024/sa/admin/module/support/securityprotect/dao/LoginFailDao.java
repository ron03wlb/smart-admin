package net.lab1024.sa.admin.module.support.securityprotect.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.admin.module.support.securityprotect.domain.entity.LoginFailEntity;
import net.lab1024.sa.admin.module.support.securityprotect.domain.form.LoginFailQueryForm;
import net.lab1024.sa.admin.module.support.securityprotect.domain.vo.LoginFailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 登录失败
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022/07/22 19:46:23 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface LoginFailDao extends BaseMapper<LoginFailEntity> {

  /**
   * 根据用户id和类型查询
   *
   * @param userId 用户ID
   * @param userType 用户类型
   * @return 登录失败记录
   */
  LoginFailEntity selectByUserIdAndUserType(
      @Param("userId") Long userId, @Param("userType") Integer userType);

  /**
   * 根据用户id和类型查询 进行删除
   *
   * @param userId 用户ID
   * @param userType 用户类型
   */
  void deleteByUserIdAndUserType(@Param("userId") Long userId, @Param("userType") Integer userType);

  /**
   * 分页查询
   *
   * @param page 分页参数
   * @param queryForm 查询表单
   * @return 登录失败VO列表
   */
  List<LoginFailVO> queryPage(Page<?> page, @Param("queryForm") LoginFailQueryForm queryForm);
}
