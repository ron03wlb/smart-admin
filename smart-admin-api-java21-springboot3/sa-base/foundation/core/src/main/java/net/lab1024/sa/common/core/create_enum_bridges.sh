#!/bin/bash

# DataTypeEnum
cat > enumeration/DataTypeEnum.java << 'JAVA'
package net.lab1024.sa.common.core.enumeration;

/**
 * @deprecated Use {@link net.lab1024.sa.foundation.domain.enumeration.DataTypeEnum} instead.
 */
@Deprecated(since = "3.6.0", forRemoval = true)
public enum DataTypeEnum implements BaseEnum {
  NORMAL(1, "普通数据"),
  ENCRYPT(10, "加密数据");
  
  private final net.lab1024.sa.foundation.domain.enumeration.DataTypeEnum delegate;
  
  DataTypeEnum(Integer value, String desc) {
    this.delegate = net.lab1024.sa.foundation.domain.enumeration.DataTypeEnum.valueOf(this.name());
  }
  
  @Override
  public Object getValue() {
    return delegate.getValue();
  }
  
  @Override
  public String getDesc() {
    return delegate.getDesc();
  }
}
JAVA

# GenderEnum
cat > enumeration/GenderEnum.java << 'JAVA'
package net.lab1024.sa.common.core.enumeration;

/**
 * @deprecated Use {@link net.lab1024.sa.foundation.domain.enumeration.GenderEnum} instead.
 */
@Deprecated(since = "3.6.0", forRemoval = true)
public enum GenderEnum implements BaseEnum {
  UNKNOWN(0, "未知"),
  MAN(1, "男"),
  WOMAN(2, "女");
  
  private final net.lab1024.sa.foundation.domain.enumeration.GenderEnum delegate;
  
  GenderEnum(Integer value, String desc) {
    this.delegate = net.lab1024.sa.foundation.domain.enumeration.GenderEnum.valueOf(this.name());
  }
  
  @Override
  public Object getValue() {
    return delegate.getValue();
  }
  
  @Override
  public String getDesc() {
    return delegate.getDesc();
  }
}
JAVA

# UserTypeEnum
cat > enumeration/UserTypeEnum.java << 'JAVA'
package net.lab1024.sa.common.core.enumeration;

/**
 * @deprecated Use {@link net.lab1024.sa.foundation.domain.enumeration.UserTypeEnum} instead.
 */
@Deprecated(since = "3.6.0", forRemoval = true)
public enum UserTypeEnum implements BaseEnum {
  ADMIN_EMPLOYEE(1, "员工");
  
  private final net.lab1024.sa.foundation.domain.enumeration.UserTypeEnum delegate;
  
  UserTypeEnum(Integer type, String desc) {
    this.delegate = net.lab1024.sa.foundation.domain.enumeration.UserTypeEnum.valueOf(this.name());
  }
  
  @Override
  public Object getValue() {
    return delegate.getValue();
  }
  
  @Override
  public String getDesc() {
    return delegate.getDesc();
  }
}
JAVA

# Constants
cat > constant/RequestHeaderConst.java << 'JAVA'
package net.lab1024.sa.common.core.constant;

/**
 * @deprecated Use {@link net.lab1024.sa.foundation.domain.constant.RequestHeaderConst} instead.
 */
@Deprecated(since = "3.6.0", forRemoval = true)
public class RequestHeaderConst extends net.lab1024.sa.foundation.domain.constant.RequestHeaderConst {
}
JAVA

cat > constant/StringConst.java << 'JAVA'
package net.lab1024.sa.common.core.constant;

/**
 * @deprecated Use {@link net.lab1024.sa.foundation.domain.constant.StringConst} instead.
 */
@Deprecated(since = "3.6.0", forRemoval = true)
public class StringConst extends net.lab1024.sa.foundation.domain.constant.StringConst {
}
JAVA

# UnexpectedErrorCode
cat > code/UnexpectedErrorCode.java << 'JAVA'
package net.lab1024.sa.common.core.code;

/**
 * @deprecated Use {@link net.lab1024.sa.foundation.domain.code.UnexpectedErrorCode} instead.
 */
@Deprecated(since = "3.6.0", forRemoval = true)
public enum UnexpectedErrorCode implements ErrorCode {
  BUSINESS_HANDING(20001, "呃~ 业务繁忙，请稍后重试"),
  PAY_ORDER_ID_ERROR(20002, "付款单id发生了异常，请联系技术人员排查");
  
  private final net.lab1024.sa.foundation.domain.code.UnexpectedErrorCode delegate;
  
  UnexpectedErrorCode(int code, String msg) {
    this.delegate = net.lab1024.sa.foundation.domain.code.UnexpectedErrorCode.valueOf(this.name());
  }
  
  @Override
  public int getCode() {
    return delegate.getCode();
  }
  
  @Override
  public String getMsg() {
    return delegate.getMsg();
  }
  
  @Override
  public String getLevel() {
    return delegate.getLevel();
  }
}
JAVA

echo "All enum and constant bridges created"
