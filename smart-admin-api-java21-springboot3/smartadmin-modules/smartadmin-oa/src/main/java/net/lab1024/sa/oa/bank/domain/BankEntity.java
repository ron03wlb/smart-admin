package net.lab1024.sa.oa.bank.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;
import net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler;
import net.lab1024.sa.support.datatracer.annoation.DataTracerFieldLabel;

/**
 * OA办公-OA银行信息
 *
 * @author 1024创新实验室:善逸
 * @since 2022/6/23 21:59:22 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_oa_bank")
public class BankEntity extends SmartAdminBaseEntity {

  /** 银行信息ID */
  @TableId(type = IdType.AUTO)
  @DataTracerFieldLabel("银行信息ID")
  private Long bankId;

  /** 开户银行 */
  @DataTracerFieldLabel("开户银行")
  private String bankName;

  /** 账户名称 */
  @DataTracerFieldLabel("账户名称")
  private String accountName;

  /** 账号 */
  @DataTracerFieldLabel("账号")
  private String accountNumber;

  /** 备注 */
  @DataTracerFieldLabel("备注")
  private String remark;

  /** 是否对公 */
  @DataTracerFieldLabel("是否对公")
  private Boolean businessFlag;

  /** 企业ID */
  private Long enterpriseId;

  /** 禁用状态 */
  @DataTracerFieldLabel("禁用状态")
  @TableField(value = "disabled", typeHandler = BooleanToSmallintTypeHandler.class)
  private Boolean disabledFlag;

  /** 删除状态 */
  @TableField(value = "deleted", typeHandler = BooleanToSmallintTypeHandler.class)
  private Boolean deletedFlag;

  /** 创建人ID */
  private Long createUserId;

  /** 创建人ID */
  private String createUserName;
}
