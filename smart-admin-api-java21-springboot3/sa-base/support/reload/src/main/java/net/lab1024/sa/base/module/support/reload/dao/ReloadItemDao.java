package net.lab1024.sa.base.module.support.reload.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import net.lab1024.sa.base.module.support.reload.domain.ReloadItemEntity;
import net.lab1024.sa.base.module.support.reload.domain.ReloadItemVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * t_reload_item 数据表dao
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2015-03-02 19:11:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface ReloadItemDao extends BaseMapper<ReloadItemEntity> {

  List<ReloadItemVO> query();
}
