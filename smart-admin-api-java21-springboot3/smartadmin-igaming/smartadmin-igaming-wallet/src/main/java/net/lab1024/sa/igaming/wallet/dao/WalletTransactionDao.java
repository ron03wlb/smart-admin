package net.lab1024.sa.igaming.wallet.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletTransactionQueryForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletTransactionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Wallet transaction data access object.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Mapper
public interface WalletTransactionDao extends BaseMapper<WalletTransactionEntity> {

  /**
   * Paginated transaction query with time range filter.
   *
   * @param page pagination parameter
   * @param query query conditions
   * @return transaction VO list
   */
  List<WalletTransactionVO> queryPage(
      Page<?> page, @Param("query") WalletTransactionQueryForm query);
}
