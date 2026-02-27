package net.lab1024.sa.igaming.game.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.lab1024.sa.igaming.game.domain.entity.GameRoundEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Game round DAO.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Mapper
public interface GameRoundDao extends BaseMapper<GameRoundEntity> {

  GameRoundEntity selectByTransactionId(@Param("transactionId") String transactionId);

  List<GameRoundEntity> selectUnsettledRoundsOlderThan(@Param("minutes") int minutes);

  List<GameRoundEntity> selectByProviderAndDate(
      @Param("providerCode") String providerCode, @Param("date") LocalDate date);

  BigDecimal sumWeightedTurnoverByPlayer(
      @Param("playerId") Long playerId, @Param("tenantId") Long tenantId);
}
