package net.lab1024.sa.system.adapter;

import io.vavr.control.Option;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.api.system.contract.PositionContract;
import net.lab1024.sa.api.system.dto.PositionDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.system.position.dao.PositionDao;
import org.springframework.stereotype.Component;

/**
 * Position Contract Adapter
 *
 * <p>Adapts PositionService to PositionContract API.
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Component
@RequiredArgsConstructor
public class PositionContractAdapter implements PositionContract {

  private final PositionDao positionDao;

  @Override
  public Option<PositionDTO> getById(Long positionId) {
    if (positionId == null) {
      throw new IllegalArgumentException("positionId cannot be null");
    }

    return Option.of(positionDao.selectById(positionId))
        .map(entity -> SmartBeanUtil.copy(entity, PositionDTO.class));
  }

  @Override
  public List<PositionDTO> listAll() {
    return positionDao.selectList(null).stream()
        .map(entity -> SmartBeanUtil.copy(entity, PositionDTO.class))
        .collect(Collectors.toList());
  }
}
