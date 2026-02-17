package net.lab1024.sa.igaming.wallet.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletBonusExtEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Wallet bonus extension data access object.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Mapper
public interface WalletBonusExtDao extends BaseMapper<WalletBonusExtEntity> {}
