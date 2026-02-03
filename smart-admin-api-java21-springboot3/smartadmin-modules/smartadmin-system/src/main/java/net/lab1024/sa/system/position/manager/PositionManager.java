package net.lab1024.sa.system.position.manager;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.lab1024.sa.system.position.dao.PositionDao;
import net.lab1024.sa.system.position.domain.entity.PositionEntity;
import org.springframework.stereotype.Service;

/**
 * 职务表 Manager
 *
 * @author kaiyun
 * @since 2024-06-23 23:31:38 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
public class PositionManager extends ServiceImpl<PositionDao, PositionEntity> {}
