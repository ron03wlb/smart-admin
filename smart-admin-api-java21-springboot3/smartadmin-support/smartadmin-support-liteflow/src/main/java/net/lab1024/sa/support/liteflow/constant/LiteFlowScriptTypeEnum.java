package net.lab1024.sa.support.liteflow.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * LiteFlow 腳本類型枚舉
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Getter
@AllArgsConstructor
public enum LiteFlowScriptTypeEnum {

  /** QLExpress 腳本（阿里巴巴表達式引擎） */
  QLEXPRESS("qlexpress", "QLExpress腳本"),

  /** Groovy 腳本 */
  GROOVY("groovy", "Groovy腳本"),

  /** JavaScript 腳本 */
  JAVASCRIPT("javascript", "JavaScript腳本");

  /** 腳本類型代碼 */
  private final String code;

  /** 腳本類型描述 */
  private final String desc;
}
