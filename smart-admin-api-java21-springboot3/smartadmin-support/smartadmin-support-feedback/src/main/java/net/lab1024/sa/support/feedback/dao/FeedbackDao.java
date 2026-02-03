package net.lab1024.sa.support.feedback.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.support.feedback.domain.FeedbackEntity;
import net.lab1024.sa.support.feedback.domain.FeedbackQueryForm;
import net.lab1024.sa.support.feedback.domain.FeedbackVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 意见反馈 dao
 *
 * @author 1024创新实验室: 开云
 * @since 2022-08-11 20:48:09 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface FeedbackDao extends BaseMapper<FeedbackEntity> {

  /** 分页查询 */
  List<FeedbackVO> queryPage(Page page, @Param("query") FeedbackQueryForm query);
}
