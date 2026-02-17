package net.lab1024.sa.igaming.wallet.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletQueryForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Wallet data access object.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Mapper
public interface WalletDao extends BaseMapper<WalletEntity> {

  /**
   * Paginated wallet query with available balance calculation.
   *
   * @param page pagination parameter
   * @param query query conditions
   * @return wallet VO list
   */
  List<WalletVO> queryPage(Page<?> page, @Param("query") WalletQueryForm query);
}
