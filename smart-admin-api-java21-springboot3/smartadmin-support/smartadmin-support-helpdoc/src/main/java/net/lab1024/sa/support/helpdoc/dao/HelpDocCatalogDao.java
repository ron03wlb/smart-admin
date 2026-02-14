package net.lab1024.sa.support.helpdoc.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.support.helpdoc.domain.entity.HelpDocCatalogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帮助文档目录
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-20 23:11:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface HelpDocCatalogDao extends BaseMapper<HelpDocCatalogEntity> {
  // empty
}
