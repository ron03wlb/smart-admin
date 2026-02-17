package net.lab1024.sa.igaming.wallet.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletLockEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Wallet lock data access object.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Mapper
public interface WalletLockDao extends BaseMapper<WalletLockEntity> {}
