package net.lab1024.sa.igaming.wallet.payment.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PspEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * PSP (Payment Service Provider) data access object.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Mapper
public interface PspDao extends BaseMapper<PspEntity> {}
