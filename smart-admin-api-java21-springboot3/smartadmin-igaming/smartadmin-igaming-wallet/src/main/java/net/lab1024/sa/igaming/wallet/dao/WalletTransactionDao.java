package net.lab1024.sa.igaming.wallet.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletTransactionQueryForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletTransactionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

  /**
   * Sum absolute amounts for a given transaction type within a date range.
   *
   * @param transactionType transaction type value
   * @param startTime range start (inclusive)
   * @param endTime range end (exclusive)
   * @return total absolute amount, or null if no records
   */
  @Select(
      "SELECT COALESCE(SUM(ABS(amount)), 0) FROM t_wallet_transaction"
          + " WHERE transaction_type = #{transactionType}"
          + " AND create_time >= #{startTime}"
          + " AND create_time < #{endTime}")
  BigDecimal sumAmountByType(
      @Param("transactionType") Integer transactionType,
      @Param("startTime") OffsetDateTime startTime,
      @Param("endTime") OffsetDateTime endTime);
}
