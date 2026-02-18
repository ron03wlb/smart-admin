package net.lab1024.sa.igaming.wallet.payment.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;
import net.lab1024.sa.igaming.wallet.payment.domain.form.PaymentOrderQueryForm;
import net.lab1024.sa.igaming.wallet.payment.domain.vo.PaymentOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Payment order data access object.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Mapper
public interface PaymentOrderDao extends BaseMapper<PaymentOrderEntity> {

  /**
   * Paginated payment order query.
   *
   * @param page pagination parameter
   * @param query query conditions
   * @return payment order VO list
   */
  List<PaymentOrderVO> queryPage(Page<?> page, @Param("query") PaymentOrderQueryForm query);
}
