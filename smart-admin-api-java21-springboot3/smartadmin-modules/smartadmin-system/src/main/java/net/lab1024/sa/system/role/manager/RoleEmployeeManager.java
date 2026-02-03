package net.lab1024.sa.system.role.manager;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.lab1024.sa.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.system.role.domain.entity.RoleEmployeeEntity;
import org.springframework.stereotype.Service;

/**
 * 角色员工 manager
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-04-08 21:53:04 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
public class RoleEmployeeManager extends ServiceImpl<RoleEmployeeDao, RoleEmployeeEntity> {}
