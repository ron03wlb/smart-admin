package net.lab1024.sa.igaming.wallet.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletQueryForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

  /**
   * Load wallet with DB pessimistic lock (SELECT FOR UPDATE).
   *
   * <p>Must be called inside a {@code @Transactional} method. The row lock is held until
   * transaction commit/rollback. This is Layer 2 of the 3-layer concurrency control.
   *
   * @param playerId player ID
   * @param walletType wallet type value (e.g. CASH=1)
   * @return wallet entity with exclusive row lock, or null if not found
   */
  @Select(
      "SELECT * FROM t_wallet WHERE player_id = #{playerId} AND wallet_type = #{walletType}"
          + " AND deleted = false FOR UPDATE")
  WalletEntity selectForUpdate(
      @Param("playerId") Long playerId, @Param("walletType") Integer walletType);
}
