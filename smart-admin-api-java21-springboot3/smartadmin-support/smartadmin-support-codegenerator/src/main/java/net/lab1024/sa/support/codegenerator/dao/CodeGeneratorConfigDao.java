package net.lab1024.sa.support.codegenerator.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.support.codegenerator.domain.entity.CodeGeneratorConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 表的 代码生成配置 Dao
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-09-23 20:15:38 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface CodeGeneratorConfigDao extends BaseMapper<CodeGeneratorConfigEntity> {
  // empty
}
