package net.lab1024.sa.igaming.wallet.payment.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentReconciliationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Payment reconciliation DAO.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@Mapper
public interface PaymentReconciliationDao extends BaseMapper<PaymentReconciliationEntity> {}
